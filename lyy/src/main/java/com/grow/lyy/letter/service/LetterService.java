package com.grow.lyy.letter.service;


import com.grow.lyy.letter.vo.LetterVO;
import com.grow.lyy.letter.dto.LetterAdminDTO;
import com.grow.lyy.letter.dto.LetterSortDTO;
import com.grow.lyy.letter.vo.LetterAdminVO;

import java.util.List;


public interface LetterService {


    /**
     * 查询信箱
     */
    List<LetterVO> getMyLetters(Long userId);


    LetterVO getLetterDetail(Long userId, Long letterId);

    LetterVO completeLetter(Long userId, Long letterId);

    List<LetterAdminVO> getAllLettersForAdmin();

    LetterAdminVO createLetterForAdmin(LetterAdminDTO dto);

    LetterAdminVO updateLetterForAdmin(Long id, LetterAdminDTO dto);

    void deleteLetterForAdmin(Long id);

    void updateLetterSortForAdmin(List<LetterSortDTO> updates);

}
