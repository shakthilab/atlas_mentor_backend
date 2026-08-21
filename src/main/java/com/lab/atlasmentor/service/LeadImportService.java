package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.LeadImportRawRow;
import com.lab.atlasmentor.dto.LeadImportResponse;
import com.lab.atlasmentor.dto.LeadImportRowResult;
import com.lab.atlasmentor.dto.StudentOnboardingRequest;
import com.lab.atlasmentor.enums.LeadBackground;
import com.lab.atlasmentor.enums.LeadImportField;
import com.lab.atlasmentor.enums.LeadPriority;
import com.lab.atlasmentor.enums.LeadPrioritySubCategory;
import com.lab.atlasmentor.exception.BusinessException;
import com.lab.atlasmentor.model.Student;
import com.lab.atlasmentor.repository.CountryRepository;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.repository.UniversityRepository;
import com.lab.atlasmentor.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Orchestrates Parts 3-5: column-mapped rows in, per-row validation + duplicate check +
 * student creation, full synchronous result out (no job table — Part 5 explicitly rules
 * that out for this version).
 *
 * Row creation reuses {@code StudentService#createOrUpdateStudent} — the exact method
 * behind POST /api/students/onboarding — rather than parallel insert logic. The request
 * built per row carries personalInfo, destinationDetails, academicHistory, and source (all
 * columns from {@link LeadImportField}, including leadClassification); documents are never
 * part of an import row.
 *
 * One deliberate deviation from a byte-for-byte "just call the shared method for every
 * row": createOrUpdateStudent's own duplicate branch (email-then-phone match) doesn't skip
 * the match, it *overwrites* it — reassigns branch/counsellor, and unconditionally clears
 * notes/course/intake and deletes+re-saves academic history/documents from whatever the
 * new request carries (see StudentService#updateStudentData). For the onboarding form
 * that's fine — a human is knowingly editing one specific record. For a bulk import, a
 * spreadsheet row that happens to share a phone/email with an existing lead — quite
 * possible at real-world import volumes — would otherwise silently reassign that lead to
 * the importer's branch/counsellor and blank out whatever another counsellor had already
 * filled in. So duplicates are detected here (identical email-then-phone precedence
 * StudentService itself uses) and reported instead of reaching that overwrite path;
 * createOrUpdateStudent is only ever invoked on the create-new branch. Flag back if
 * imports should instead be allowed to merge into existing records.
 */
@Service
public class LeadImportService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final LeadImportParseService parseService;
    private final LeadLinkResolverService linkResolverService;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final CountryRepository countryRepository;
    private final UniversityRepository universityRepository;

    public LeadImportService(LeadImportParseService parseService,
                              LeadLinkResolverService linkResolverService,
                              StudentService studentService,
                              StudentRepository studentRepository,
                              CountryRepository countryRepository,
                              UniversityRepository universityRepository) {
        this.parseService = parseService;
        this.linkResolverService = linkResolverService;
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.countryRepository = countryRepository;
        this.universityRepository = universityRepository;
    }

    public LeadImportResponse importFromFile(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        List<LeadImportRawRow> rawRows;
        try {
            if (filename.endsWith(".csv")) {
                rawRows = parseService.parseCsv(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                rawRows = parseService.parseExcel(file.getInputStream());
            } else {
                throw new BusinessException("Unsupported file type. Please upload a .xlsx, .xls, or .csv file.");
            }
        } catch (IOException e) {
            throw new BusinessException("Could not read the uploaded file.");
        }
        return process(rawRows, "file:" + file.getOriginalFilename());
    }

    public LeadImportResponse importFromLink(String url) {
        List<LeadImportRawRow> rawRows = linkResolverService.resolveAndParse(url);
        return process(rawRows, "link:" + url);
    }

    private LeadImportResponse process(List<LeadImportRawRow> rawRows, String sourceRef) {
        if (rawRows.isEmpty()) {
            throw new BusinessException("No data rows found to import.");
        }

        // Never taken from the sheet/file content or a request parameter — always the
        // importing employee's own JWT claims.
        Long branchId = SecurityUtils.getCurrentBranchId();
        Long counsellorId = SecurityUtils.getCurrentUserId();

        List<LeadImportRowResult> results = new ArrayList<>();
        int succeeded = 0;
        int duplicates = 0;
        int failed = 0;

        for (LeadImportRawRow raw : rawRows) {
            LeadImportRowResult result = new LeadImportRowResult();
            result.setRow(raw.rowNumber());
            try {
                String firstName = raw.values().get(LeadImportField.FIRST_NAME);
                String lastName = raw.values().get(LeadImportField.LAST_NAME);
                String countryCode = raw.values().get(LeadImportField.COUNTRY_CODE);
                String rawPhone = raw.values().get(LeadImportField.PHONE);
                String email = raw.values().get(LeadImportField.EMAIL);

                if (firstName == null || firstName.isBlank()) {
                    throw new RowValidationException("missing first name");
                }
                if (lastName == null || lastName.isBlank()) {
                    throw new RowValidationException("missing last name");
                }
                if (countryCode == null || countryCode.isBlank()) {
                    throw new RowValidationException("missing country code");
                }
                if (rawPhone == null || rawPhone.isBlank()) {
                    throw new RowValidationException("missing phone number");
                }
                String phone = normalizeAndValidatePhone(rawPhone);
                if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
                    throw new RowValidationException("invalid email format: " + email);
                }

                Student existing = findExisting(email, phone);
                if (existing != null) {
                    result.setStatus(LeadImportRowResult.Status.DUPLICATE);
                    result.setMessage("Duplicate of existing lead #" + existing.getId()
                            + " — row skipped, existing record left unchanged.");
                    result.setStudentId(existing.getId());
                    duplicates++;
                    results.add(result);
                    continue;
                }

                StudentOnboardingRequest request = buildRequest(raw, firstName.trim(), lastName.trim(),
                        countryCode.trim(), phone, email, branchId, counsellorId);
                Student created = studentService.createOrUpdateStudent(request);

                result.setStatus(LeadImportRowResult.Status.SUCCESS);
                result.setMessage("Lead created");
                result.setStudentId(created.getId());
                succeeded++;
            } catch (RowValidationException e) {
                result.setStatus(LeadImportRowResult.Status.FAILED);
                result.setMessage(e.getMessage());
                failed++;
            } catch (BusinessException e) {
                result.setStatus(LeadImportRowResult.Status.FAILED);
                result.setMessage(e.getMessage());
                failed++;
            } catch (Exception e) {
                result.setStatus(LeadImportRowResult.Status.FAILED);
                result.setMessage("unexpected error: " + e.getMessage());
                failed++;
            }
            results.add(result);
        }

        LeadImportResponse response = new LeadImportResponse();
        response.setSource(sourceRef);
        response.setTotalRows(rawRows.size());
        response.setSucceeded(succeeded);
        response.setDuplicates(duplicates);
        response.setFailed(failed);
        response.setResults(results);
        return response;
    }

    /** Same email-then-phone precedence as StudentService#createOrUpdateStudent. */
    private Student findExisting(String email, String phone) {
        Student existing = null;
        if (email != null && !email.isBlank()) {
            existing = studentRepository.findByEmail(email.trim()).orElse(null);
        }
        if (existing == null) {
            existing = studentRepository.findByPhone(phone).orElse(null);
        }
        return existing;
    }

    private StudentOnboardingRequest buildRequest(LeadImportRawRow raw, String firstName, String lastName,
                                                    String countryCode, String phone, String email,
                                                    Long branchId, Long counsellorId) {
        StudentOnboardingRequest.PersonalInfo personalInfo = new StudentOnboardingRequest.PersonalInfo();
        personalInfo.setFirstName(firstName);
        personalInfo.setLastName(lastName);
        personalInfo.setCountryCode(countryCode);
        personalInfo.setPhone(phone);
        personalInfo.setEmail(email != null ? email.trim() : null);
        // personalInfo.branchId / .counsellor: always the caller's own JWT claims —
        // never the sheet/file content, never a request parameter.
        personalInfo.setBranchId(branchId);
        personalInfo.setCounsellor(counsellorId);

        String source = raw.values().get(LeadImportField.SOURCE);

        StudentOnboardingRequest request = new StudentOnboardingRequest();
        request.setPersonalInfo(personalInfo);
        request.setDestinationDetails(buildDestinationDetails(raw));
        request.setAcademicHistory(buildAcademicHistory(raw));
        request.setSource(isBlank(source) ? null : source.trim());
        request.setLeadClassification(buildLeadClassification(raw));
        return request;
    }

    /**
     * Priority / Priority Subcategory / Background, validated against the same
     * LeadPriority/LeadPrioritySubCategory/LeadBackground enums the manual create/edit endpoint
     * and the template generator use — a mismatched or unrecognized value fails the row with a
     * specific reason rather than being silently dropped or silently corrected, same as the
     * other required-field validation in this class. Case-insensitive on Priority and Priority
     * Subcategory; Background accepts "Educated"/"Less Educated" case-insensitively too (all
     * via each enum's fromText/fromTextAnyTier).
     */
    private StudentOnboardingRequest.LeadClassification buildLeadClassification(LeadImportRawRow raw) {
        String priorityRaw = raw.values().get(LeadImportField.PRIORITY);
        String subCategoryRaw = raw.values().get(LeadImportField.PRIORITY_SUB_CATEGORY);
        String backgroundRaw = raw.values().get(LeadImportField.BACKGROUND);
        if (isBlank(priorityRaw) && isBlank(subCategoryRaw) && isBlank(backgroundRaw)) {
            return null;
        }

        StudentOnboardingRequest.LeadClassification classification = new StudentOnboardingRequest.LeadClassification();

        LeadPriority priority = null;
        if (!isBlank(priorityRaw)) {
            priority = LeadPriority.fromText(priorityRaw);
            if (priority == null) {
                throw new RowValidationException(
                        "'" + priorityRaw.trim() + "' is not a valid Priority (expected P1, P2, or P3)");
            }
            classification.setPriority(priority);
        }

        if (!isBlank(subCategoryRaw)) {
            LeadPrioritySubCategory subCategory = LeadPrioritySubCategory.fromTextAnyTier(subCategoryRaw);
            if (subCategory == null) {
                throw new RowValidationException(
                        "'" + subCategoryRaw.trim() + "' is not a recognized Priority Subcategory");
            }
            try {
                LeadPrioritySubCategory.requireBelongsToTier(subCategory, priority);
            } catch (IllegalArgumentException e) {
                throw new RowValidationException(e.getMessage());
            }
            classification.setPrioritySubCategory(subCategory);
        }

        if (!isBlank(backgroundRaw)) {
            LeadBackground background = LeadBackground.fromText(backgroundRaw);
            if (background == null) {
                throw new RowValidationException("'" + backgroundRaw.trim()
                        + "' is not a valid Background (expected Educated or Less Educated)");
            }
            classification.setBackground(background);
        }

        return classification;
    }

    /**
     * Destination Country / Target University are foreign keys on the created lead, resolved
     * here by case-insensitive name lookup against the existing Country/University tables. A
     * name that doesn't match anything leaves the corresponding id null — it's a soft, partial
     * gap (see class javadoc / the prompt this was built against), never a reason to fail the
     * row; Course Name and Intake Period are always free text, stored exactly as given.
     */
    private StudentOnboardingRequest.DestinationDetails buildDestinationDetails(LeadImportRawRow raw) {
        String countryName = raw.values().get(LeadImportField.DESTINATION_COUNTRY);
        String universityName = raw.values().get(LeadImportField.TARGET_UNIVERSITY);
        String course = raw.values().get(LeadImportField.COURSE_NAME);
        String intake = raw.values().get(LeadImportField.INTAKE_PERIOD);
        if (isBlank(countryName) && isBlank(universityName) && isBlank(course) && isBlank(intake)) {
            return null;
        }

        StudentOnboardingRequest.DestinationDetails details = new StudentOnboardingRequest.DestinationDetails();
        if (!isBlank(countryName)) {
            countryRepository.findByNameIgnoreCase(countryName.trim())
                    .ifPresent(country -> details.setCountryId(country.getId()));
        }
        if (!isBlank(universityName)) {
            universityRepository.findByNameIgnoreCase(universityName.trim())
                    .ifPresent(university -> details.setUniversityId(university.getId()));
        }
        details.setCourse(isBlank(course) ? null : course.trim());
        details.setIntake(isBlank(intake) ? null : intake.trim());
        return details;
    }

    private List<StudentOnboardingRequest.AcademicEntry> buildAcademicHistory(LeadImportRawRow raw) {
        StudentOnboardingRequest.AcademicEntry tenth = buildAcademicEntry(raw, "10th",
                LeadImportField.TENTH_INSTITUTION, LeadImportField.TENTH_PASSING_YEAR, LeadImportField.TENTH_SCORE);
        StudentOnboardingRequest.AcademicEntry twelfth = buildAcademicEntry(raw, "12th",
                LeadImportField.TWELFTH_INSTITUTION, LeadImportField.TWELFTH_PASSING_YEAR, LeadImportField.TWELFTH_SCORE);
        if (tenth == null && twelfth == null) {
            return null;
        }
        List<StudentOnboardingRequest.AcademicEntry> entries = new ArrayList<>();
        if (tenth != null) entries.add(tenth);
        if (twelfth != null) entries.add(twelfth);
        return entries;
    }

    /** Institution and Score/CGPA are free text, stored exactly as provided — no validation
     * beyond basic presence. Passing Year is the one sub-field with a real type constraint
     * underneath (an Integer column); an unparseable year is left null rather than failing
     * the row, same soft-fail principle as the country/university lookups above. */
    private StudentOnboardingRequest.AcademicEntry buildAcademicEntry(LeadImportRawRow raw, String qualification,
            LeadImportField institutionField, LeadImportField yearField, LeadImportField scoreField) {
        String institution = raw.values().get(institutionField);
        String yearRaw = raw.values().get(yearField);
        String score = raw.values().get(scoreField);
        if (isBlank(institution) && isBlank(yearRaw) && isBlank(score)) {
            return null;
        }

        StudentOnboardingRequest.AcademicEntry entry = new StudentOnboardingRequest.AcademicEntry();
        entry.setQualification(qualification);
        entry.setInstitutionName(isBlank(institution) ? null : institution.trim());
        entry.setPassingYear(parseYear(yearRaw));
        entry.setScore(isBlank(score) ? null : score.trim());
        return entry;
    }

    private Integer parseYear(String yearRaw) {
        if (isBlank(yearRaw)) {
            return null;
        }
        try {
            return Integer.valueOf(yearRaw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeAndValidatePhone(String rawPhone) {
        String digitsOnly = rawPhone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
            throw new RowValidationException("invalid phone number: " + rawPhone);
        }
        return rawPhone.trim();
    }

    private static final class RowValidationException extends RuntimeException {
        RowValidationException(String message) {
            super(message);
        }
    }
}
