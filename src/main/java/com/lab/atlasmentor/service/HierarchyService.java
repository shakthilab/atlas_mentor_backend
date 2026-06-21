package com.lab.atlasmentor.service;

import com.lab.atlasmentor.dto.CounsellorHierarchyResponse;
import com.lab.atlasmentor.dto.ManagerHierarchyResponse;
import com.lab.atlasmentor.model.Branch;
import com.lab.atlasmentor.model.CounsellorHierarchy;
import com.lab.atlasmentor.model.ManagerEmployeeHierarchy;
import com.lab.atlasmentor.model.ReferralAssignment;
import com.lab.atlasmentor.model.User;
import com.lab.atlasmentor.repository.BranchRepository;
import com.lab.atlasmentor.repository.CompanyDetailsRepository;
import com.lab.atlasmentor.repository.CounsellorHierarchyRepository;
import com.lab.atlasmentor.repository.EmployeeDetailsRepository;
import com.lab.atlasmentor.repository.ManagerEmployeeHierarchyRepository;
import com.lab.atlasmentor.repository.ReferralAssignmentRepository;
import com.lab.atlasmentor.repository.StudentRepository;
import com.lab.atlasmentor.repository.UserRepository;
import com.lab.atlasmentor.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HierarchyService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounsellorHierarchyRepository counsellorHierarchyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ManagerEmployeeHierarchyRepository managerEmployeeHierarchyRepository;

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    @Autowired
    private ReferralAssignmentRepository referralAssignmentRepository;

    @Autowired
    private EmployeeDetailsRepository employeeDetailsRepository;

    @Transactional
    public void assignEmployeeToManager(Long employeeId, Long managerId, Long currentUserId) {
        log.info("[assignEmployee] START employeeId={} managerId={} currentUserId={}", employeeId, managerId, currentUserId);

        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("User not found: " + employeeId));
        log.info("[assignEmployee] user found: id={} role={}", user.getId(), user.getRole() != null ? user.getRole().getName() : "null");

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));
        log.info("[assignEmployee] manager found: id={}", manager.getId());

        log.info("[assignEmployee] deleting existing assignment for employeeId={}", employeeId);
        managerEmployeeHierarchyRepository.deleteDirectlyByEmployeeId(employeeId);

        log.info("[assignEmployee] saving new ManagerEmployeeHierarchy managerId={} employeeId={}", managerId, employeeId);
        managerEmployeeHierarchyRepository.save(new ManagerEmployeeHierarchy(managerId, employeeId));

        if (user.hasRole("COMPANY")) {
            log.info("[assignEmployee] user is COMPANY, updating companyDetails");
            companyDetailsRepository.findByUserId(employeeId).ifPresent(cd -> {
                cd.setAssignedTo(manager);
                companyDetailsRepository.save(cd);
            });
        }

        if (user.hasRole("REFERRAL")) {
            log.info("[assignEmployee] user is REFERRAL, updating referralAssignment");
            referralAssignmentRepository.deleteByReferralId(employeeId);
            ReferralAssignment ra = new ReferralAssignment(user, manager);
            ra.setCreatedBy(currentUserId);
            ra.setUpdatedBy(currentUserId);
            referralAssignmentRepository.save(ra);
        }

        log.info("[assignEmployee] updating employeeDetails.assignedTo");
        employeeDetailsRepository.findByUserId(employeeId).ifPresent(ed -> {
            ed.setAssignedTo(manager);
            employeeDetailsRepository.save(ed);
        });

        log.info("[assignEmployee] DONE employeeId={} managerId={}", employeeId, managerId);
    }

    public List<ManagerHierarchyResponse> getManagerHierarchy() {
        try {
            // Get current user for branch-based filtering
            var currentUser = SecurityUtils.getCurrentUser();
            log.info("Getting manager hierarchy with branch-based access control: userId={}, role={}, branchId={}, isAdmin={}", 
                currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());
            
            List<User> managers = userRepository.findAllManagers();
            List<User> allEmployees = userRepository.findByRoleNames(List.of("SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR", "COUNSELLOR"));
            
            // Apply branch-based filtering for non-admin users
            if (!currentUser.isAdmin()) {
                Long userBranchId = currentUser.getBranchId();
                managers = managers.stream()
                    .filter(manager -> manager.getBranchId() != null && manager.getBranchId().equals(userBranchId))
                    .collect(Collectors.toList());
                
                allEmployees = allEmployees.stream()
                    .filter(employee -> employee.getBranchId() != null && employee.getBranchId().equals(userBranchId))
                    .collect(Collectors.toList());
            }
        
        // Get all branches for reference
        List<Branch> branches = branchRepository.findAll();
        Map<Long, Branch> branchMap = branches.stream()
            .collect(Collectors.toMap(Branch::getId, branch -> branch));
        
        return managers.stream().map(manager -> {
            // Get employees explicitly assigned to this manager
            List<Long> employeeIds = managerEmployeeHierarchyRepository.findEmployeeIdsByManagerId(manager.getId());
            List<User> managerEmployees = userRepository.findUsersByIds(employeeIds);
            
            List<ManagerHierarchyResponse.EmployeeDto> employeeDtos = managerEmployees.stream()
                .map(emp -> new ManagerHierarchyResponse.EmployeeDto(
                    emp.getId(),
                    emp.getFullName(),
                    emp.getEmail(),
                    emp.getPhone(),
                    emp.getStatus().toString(),
                    emp.getRole().getName()
                ))
                .collect(Collectors.toList());
            
            ManagerHierarchyResponse.BranchDto branchDto = null;
            if (manager.getBranchId() != null && branchMap.containsKey(manager.getBranchId())) {
                Branch branch = branchMap.get(manager.getBranchId());
                branchDto = new ManagerHierarchyResponse.BranchDto(
                    branch.getId(),
                    branch.getName(),
                    branch.getLocation(),
                    branch.getStatus().toString()
                );
            }
            
            return new ManagerHierarchyResponse(
                manager.getId(),
                manager.getFullName(),
                manager.getEmail(),
                manager.getPhone(),
                branchDto,
                employeeDtos
            );
        }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting current user for manager hierarchy: {}", e.getMessage(), e);
            // Fallback to original behavior without branch filtering
            List<User> managers = userRepository.findAllManagers();
            List<User> allEmployees = userRepository.findByRoleNames(List.of("SENIOR_COUNSELLOR", "JUNIOR_COUNSELLOR", "COUNSELLOR"));
            
            // Get all branches for reference
            List<Branch> branches = branchRepository.findAll();
            Map<Long, Branch> branchMap = branches.stream()
                .collect(Collectors.toMap(Branch::getId, branch -> branch));
            
            return managers.stream().map(manager -> {
                // Get employees explicitly assigned to this manager
                List<Long> employeeIds = managerEmployeeHierarchyRepository.findEmployeeIdsByManagerId(manager.getId());
                List<User> managerEmployees = userRepository.findUsersByIds(employeeIds);
                
                List<ManagerHierarchyResponse.EmployeeDto> employeeDtos = managerEmployees.stream()
                    .map(emp -> new ManagerHierarchyResponse.EmployeeDto(
                        emp.getId(),
                        emp.getFullName(),
                        emp.getEmail(),
                        emp.getPhone(),
                        emp.getStatus().toString(),
                        emp.getRole().getName()
                    ))
                    .collect(Collectors.toList());
                
                ManagerHierarchyResponse.BranchDto branchDto = null;
                if (manager.getBranchId() != null && branchMap.containsKey(manager.getBranchId())) {
                    Branch branch = branchMap.get(manager.getBranchId());
                    branchDto = new ManagerHierarchyResponse.BranchDto(
                        branch.getId(),
                        branch.getName(),
                        branch.getLocation(),
                        branch.getStatus().toString()
                    );
                }
                
                return new ManagerHierarchyResponse(
                    manager.getId(),
                    manager.getFullName(),
                    manager.getEmail(),
                    manager.getPhone(),
                    branchDto,
                    employeeDtos
                );
            }).collect(Collectors.toList());
        }
    }

    public List<CounsellorHierarchyResponse> getCounsellorHierarchy() {
        try {
            // Get current user for branch-based filtering
            var currentUser = SecurityUtils.getCurrentUser();
            log.info("Getting counsellor hierarchy with branch-based access control: userId={}, role={}, branchId={}, isAdmin={}", 
                currentUser.getUserId(), currentUser.getRole(), currentUser.getBranchId(), currentUser.isAdmin());
            
            List<User> seniorCounsellors = userRepository.findAllSeniorCounsellors();
            
            // Apply branch-based filtering for non-admin users
            if (!currentUser.isAdmin()) {
                Long userBranchId = currentUser.getBranchId();
                seniorCounsellors = seniorCounsellors.stream()
                    .filter(counsellor -> counsellor.getBranchId() != null && counsellor.getBranchId().equals(userBranchId))
                    .collect(Collectors.toList());
            }
            
            // Get all branches for reference
            List<Branch> branches = branchRepository.findAll();
            Map<Long, Branch> branchMap = branches.stream()
                .collect(Collectors.toMap(Branch::getId, branch -> branch));
        
        // Get all junior counsellors for student count mapping
        List<User> allJuniorCounsellors = userRepository.findAllJuniorCounsellors();
        Map<Long, Long> studentCountMap = allJuniorCounsellors.stream()
            .collect(Collectors.toMap(
                User::getId,
                jc -> studentRepository.countStudentsByAssignedBy(jc.getId())
            ));
        
        return seniorCounsellors.stream().map(senior -> {
            // Get junior counsellors mapped to this senior
            List<Long> juniorIds = counsellorHierarchyRepository.findJuniorCounsellorIdsBySeniorCounsellorId(senior.getId());
            List<User> juniorCounsellors = userRepository.findCounsellorsByIds(juniorIds);
            
            List<CounsellorHierarchyResponse.JuniorCounsellorDto> juniorDtos = juniorCounsellors.stream()
                .map(junior -> new CounsellorHierarchyResponse.JuniorCounsellorDto(
                    junior.getId(),
                    junior.getFullName(),
                    junior.getEmail(),
                    junior.getPhone(),
                    junior.getStatus().toString(),
                    studentCountMap.getOrDefault(junior.getId(), 0L)
                ))
                .collect(Collectors.toList());
            
            CounsellorHierarchyResponse.BranchDto branchDto = null;
            if (senior.getBranchId() != null && branchMap.containsKey(senior.getBranchId())) {
                Branch branch = branchMap.get(senior.getBranchId());
                branchDto = new CounsellorHierarchyResponse.BranchDto(
                    branch.getId(),
                    branch.getName(),
                    branch.getLocation(),
                    branch.getStatus().toString()
                );
            }
            
            return new CounsellorHierarchyResponse(
                senior.getId(),
                senior.getFullName(),
                senior.getEmail(),
                senior.getPhone(),
                branchDto,
                juniorDtos
            );
        }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting current user for counsellor hierarchy: {}", e.getMessage(), e);
            // Fallback to original behavior without branch filtering
            List<User> seniorCounsellors = userRepository.findAllSeniorCounsellors();
            
            // Get all branches for reference
            List<Branch> branches = branchRepository.findAll();
            Map<Long, Branch> branchMap = branches.stream()
                .collect(Collectors.toMap(Branch::getId, branch -> branch));
        
            // Get all junior counsellors for student count mapping
            List<User> allJuniorCounsellors = userRepository.findAllJuniorCounsellors();
            Map<Long, Long> studentCountMap = allJuniorCounsellors.stream()
                .collect(Collectors.toMap(
                    User::getId,
                    jc -> studentRepository.countStudentsByAssignedBy(jc.getId())
                ));
        
            return seniorCounsellors.stream().map(senior -> {
                // Get junior counsellors mapped to this senior
                List<Long> juniorIds = counsellorHierarchyRepository.findJuniorCounsellorIdsBySeniorCounsellorId(senior.getId());
                List<User> juniorCounsellors = userRepository.findCounsellorsByIds(juniorIds);
                
                List<CounsellorHierarchyResponse.JuniorCounsellorDto> juniorDtos = juniorCounsellors.stream()
                    .map(junior -> new CounsellorHierarchyResponse.JuniorCounsellorDto(
                        junior.getId(),
                        junior.getFullName(),
                        junior.getEmail(),
                        junior.getPhone(),
                        junior.getStatus().toString(),
                        studentCountMap.getOrDefault(junior.getId(), 0L)
                    ))
                    .collect(Collectors.toList());
                
                CounsellorHierarchyResponse.BranchDto branchDto = null;
                if (senior.getBranchId() != null && branchMap.containsKey(senior.getBranchId())) {
                    Branch branch = branchMap.get(senior.getBranchId());
                    branchDto = new CounsellorHierarchyResponse.BranchDto(
                        branch.getId(),
                        branch.getName(),
                        branch.getLocation(),
                        branch.getStatus().toString()
                    );
                }
                
                return new CounsellorHierarchyResponse(
                    senior.getId(),
                    senior.getFullName(),
                    senior.getEmail(),
                    senior.getPhone(),
                    branchDto,
                    juniorDtos
                );
            }).collect(Collectors.toList());
        }
    }
}
