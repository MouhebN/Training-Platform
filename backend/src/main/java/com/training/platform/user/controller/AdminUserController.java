package com.training.platform.user.controller;

import com.training.platform.common.response.ApiResponse;
import com.training.platform.user.dto.UserAdminResponse;
import com.training.platform.user.service.UserAccountService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAccountService userAccountService;

    public AdminUserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ApiResponse<List<UserAdminResponse>> findAll() {
        return ApiResponse.success("Users retrieved", userAccountService.findAllForAdmin());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserAdminResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("User retrieved", userAccountService.findByIdForAdmin(id));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<UserAdminResponse> activate(@PathVariable Long id) {
        return ApiResponse.success("User activated", userAccountService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<UserAdminResponse> deactivate(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("User deactivated", userAccountService.deactivate(id, principal.getName()));
    }

    @PatchMapping("/{id}/unlock")
    public ApiResponse<UserAdminResponse> unlock(@PathVariable Long id) {
        return ApiResponse.success("User unlocked", userAccountService.unlock(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        userAccountService.delete(id, principal.getName());
    }
}
