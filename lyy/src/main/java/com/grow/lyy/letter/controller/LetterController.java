package com.grow.lyy.letter.controller;


import com.grow.lyy.common.response.Result;
import com.grow.lyy.common.security.AdminAuthorizationService;
import com.grow.lyy.common.utils.SecurityUtils;
import com.grow.lyy.letter.dto.LetterAdminDTO;
import com.grow.lyy.letter.dto.LetterSortDTO;
import com.grow.lyy.letter.service.LetterService;
import com.grow.lyy.user.dto.AdminLoginDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/letter")
@RequiredArgsConstructor
public class LetterController {


    private final LetterService letterService;
    private final AdminAuthorizationService adminAuthorizationService;



    /**
     * 查看信箱
     */
    @GetMapping("/list")
    public Result<?> list(){


        Long userId =
                SecurityUtils.getUserId();


        return Result.success(
                letterService.getMyLetters(
                        userId
                )
        );

    }



    /**
     * 查看具体信件
     */
    @GetMapping("/{id}")
    public Result<?> detail(
            @PathVariable Long id
    ){


        Long userId =
                SecurityUtils.getUserId();


        return Result.success(
                letterService.getLetterDetail(
                        userId,
                        id
                )
        );

    }

    @PostMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(letterService.completeLetter(userId, id));
    }

    @GetMapping("/admin/list")
    public Result<?> adminList() {
        adminAuthorizationService.requireAdmin();
        return Result.success(letterService.getAllLettersForAdmin());
    }

    @PostMapping("/admin/login")
    public Result<?> adminLogin(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminAuthorizationService.login(dto.getUsername(), dto.getPassword()));
    }

    @PostMapping("/admin")
    public Result<?> create(@Valid @RequestBody LetterAdminDTO dto) {
        adminAuthorizationService.requireAdmin();
        return Result.success(letterService.createLetterForAdmin(dto));
    }

    @PutMapping("/admin/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody LetterAdminDTO dto) {
        adminAuthorizationService.requireAdmin();
        return Result.success(letterService.updateLetterForAdmin(id, dto));
    }

    @DeleteMapping("/admin/{id}")
    public Result<?> delete(
            @PathVariable Long id) {
        adminAuthorizationService.requireAdmin();
        letterService.deleteLetterForAdmin(id);
        return Result.success();
    }

    @PutMapping("/admin/sort")
    public Result<?> updateSort(@Valid @RequestBody List<@Valid LetterSortDTO> updates) {
        adminAuthorizationService.requireAdmin();
        letterService.updateLetterSortForAdmin(updates);
        return Result.success();
    }

}
