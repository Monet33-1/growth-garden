package com.grow.lyy.letter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.common.utils.MarkdownUtils;
import com.grow.lyy.diary.service.DiaryService;
import com.grow.lyy.letter.dto.LetterAdminDTO;
import com.grow.lyy.letter.dto.LetterSortDTO;
import com.grow.lyy.letter.entity.Letter;
import com.grow.lyy.letter.entity.UserLetter;
import com.grow.lyy.letter.mapper.LetterMapper;
import com.grow.lyy.letter.mapper.UserLetterMapper;
import com.grow.lyy.letter.service.LetterService;
import com.grow.lyy.letter.vo.LetterVO;
import com.grow.lyy.letter.vo.LetterAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class LetterServiceImpl
        implements LetterService {


    private final LetterMapper letterMapper;

    private final UserLetterMapper userLetterMapper;


    private final DiaryService diaryService;



    /**
     * 查看信箱
     */
    @Override
    public List<LetterVO> getMyLetters(Long userId) {


        List<Letter> letters =
                letterMapper.selectList(
                        new LambdaQueryWrapper<Letter>()
                                .orderByAsc(
                                        Letter::getSort
                                )
                );


        Map<Long, UserLetter> savedLetters = userLetterMapper.selectList(
                        new LambdaQueryWrapper<UserLetter>().eq(UserLetter::getUserId, userId))
                .stream().collect(Collectors.toMap(UserLetter::getLetterId, Function.identity()));

        // Locked titles, covers, and bodies are never sent to the client.
        return letters.stream()
                .map(letter -> {
                    UserLetter saved = savedLetters.get(letter.getId());
                    if (saved != null || checkUnlock(userId, letter)) {
                        return toUserVO(letter, saved);
                    }
                    LetterVO locked = new LetterVO();
                    locked.setId(letter.getId());
                    locked.setTitle("未知来信");
                    locked.setStatus("LOCKED");
                    return locked;
                })
                .toList();

    }



    /**
     * 查看具体信件
     */
    @Override
    public LetterVO getLetterDetail(
            Long userId,
            Long letterId
    ) {
        Letter letter = requireLetter(letterId);
        UserLetter saved = findSavedLetter(userId, letterId);
        if (saved == null && !checkUnlock(userId, letter)) {
            throw new BusinessException("这封信还没有到达。");
        }
        LetterVO vo = toUserVO(letter, saved);
        vo.setContent(letter.getContent());
        vo.setContentHtml(MarkdownUtils.toHtmlPreservingLineBreaks(letter.getContent()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LetterVO completeLetter(Long userId, Long letterId) {
        // Serialize concurrent completions for this letter; the unique user/letter key is the final guard.
        Letter letter = letterMapper.selectByIdForUpdate(letterId);
        if (letter == null) throw new BusinessException("信件不存在或已被移走。");

        UserLetter saved = findSavedLetter(userId, letterId);
        if (saved != null) {
            LetterVO existing = toUserVO(letter, saved);
            existing.setContent(letter.getContent());
            existing.setContentHtml(MarkdownUtils.toHtmlPreservingLineBreaks(letter.getContent()));
            return existing;
        }
        if (!checkUnlock(userId, letter)) {
            throw new BusinessException("这封信还没有到达，等花园再长大一些吧。");
        }

        com.grow.lyy.diary.entity.Diary diary = new com.grow.lyy.diary.entity.Diary();
        diary.setTitle(letter.getTitle());
        diary.setContent(letter.getContent());
        var createdDiary = diaryService.createDiary(userId, diary);

        UserLetter userLetter = new UserLetter();
        userLetter.setUserId(userId);
        userLetter.setLetterId(letterId);
        userLetter.setDiaryId(createdDiary.getId());
        userLetter.setCreateTime(LocalDateTime.now());
        userLetterMapper.insert(userLetter);

        LetterVO result = toUserVO(letter, userLetter);
        result.setContent(letter.getContent());
        result.setContentHtml(MarkdownUtils.toHtmlPreservingLineBreaks(letter.getContent()));
        return result;
    }

    private LetterVO toUserVO(Letter letter, UserLetter saved) {
        LetterVO vo = new LetterVO();
        vo.setId(letter.getId());
        vo.setTitle(letter.getTitle());
        vo.setCover(letter.getCover());
        vo.setStatus(saved == null ? "UNREAD" : "SAVED");
        vo.setDiaryId(saved == null ? null : saved.getDiaryId());
        return vo;
    }

    private UserLetter findSavedLetter(Long userId, Long letterId) {
        return userLetterMapper.selectOne(new LambdaQueryWrapper<UserLetter>()
                .eq(UserLetter::getUserId, userId)
                .eq(UserLetter::getLetterId, letterId));
    }




    @Override
    public List<LetterAdminVO> getAllLettersForAdmin() {
        return letterMapper.selectList(
                        new LambdaQueryWrapper<Letter>()
                                .orderByAsc(Letter::getSort)
                                .orderByAsc(Letter::getId))
                .stream()
                .map(this::toAdminVO)
                .toList();
    }

    @Override
    @Transactional
    public LetterAdminVO createLetterForAdmin(LetterAdminDTO dto) {
        Letter letter = new Letter();
        applyAdminFields(dto, letter);
        LocalDateTime now = LocalDateTime.now();
        letter.setCreateTime(now);
        letter.setUpdateTime(now);
        letterMapper.insert(letter);
        return toAdminVO(letter);
    }

    @Override
    @Transactional
    public LetterAdminVO updateLetterForAdmin(Long id, LetterAdminDTO dto) {
        Letter letter = requireLetter(id);
        applyAdminFields(dto, letter);
        letter.setUpdateTime(LocalDateTime.now());
        letterMapper.updateById(letter);
        return toAdminVO(letter);
    }

    @Override
    @Transactional
    public void deleteLetterForAdmin(Long id) {
        requireLetter(id);
        // Unlock state is derived from diary count; no read table or diary-to-letter relation exists.
        letterMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateLetterSortForAdmin(List<LetterSortDTO> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new BusinessException("请选择要调整排序的信件");
        }

        Set<Long> ids = new HashSet<>();
        for (LetterSortDTO update : updates) {
            if (!ids.add(update.getId())) {
                throw new BusinessException("排序列表中包含重复信件");
            }
        }

        List<Letter> letters = letterMapper.selectBatchIds(ids);
        if (letters.size() != ids.size()) {
            throw new BusinessException("部分信件已不存在，请刷新列表后重试");
        }

        var sortsById = updates.stream().collect(
                java.util.stream.Collectors.toMap(LetterSortDTO::getId, LetterSortDTO::getSort));
        LocalDateTime now = LocalDateTime.now();
        for (Letter letter : letters) {
            letter.setSort(sortsById.get(letter.getId()));
            letter.setUpdateTime(now);
            letterMapper.updateById(letter);
        }
    }

    private Letter requireLetter(Long id) {
        Letter letter = letterMapper.selectById(id);
        if (letter == null) {
            throw new BusinessException("信件不存在或已被删除");
        }
        return letter;
    }

    private void applyAdminFields(LetterAdminDTO dto, Letter letter) {
        letter.setTitle(dto.getTitle().trim());
        letter.setContent(dto.getContent());
        letter.setUnlockType(dto.getUnlockType());
        letter.setUnlockValue(dto.getUnlockValue());
        letter.setCover(dto.getCover() == null || dto.getCover().isBlank() ? null : dto.getCover().trim());
        letter.setSort(dto.getSort());
    }

    private LetterAdminVO toAdminVO(Letter letter) {
        LetterAdminVO vo = new LetterAdminVO();
        vo.setId(letter.getId());
        vo.setTitle(letter.getTitle());
        vo.setContent(letter.getContent());
        vo.setUnlockType(letter.getUnlockType());
        vo.setUnlockValue(letter.getUnlockValue());
        vo.setCover(letter.getCover());
        vo.setSort(letter.getSort());
        vo.setCreateTime(letter.getCreateTime());
        vo.setUpdateTime(letter.getUpdateTime());
        return vo;
    }

    /**
     * 判断信件是否解锁
     */
    private boolean checkUnlock(
            Long userId,
            Letter letter
    ) {


        if(
                "DIARY_COUNT"
                        .equals(
                                letter.getUnlockType()
                        )
        ){


            long diaryCount =
                    diaryService.countDiary(
                            userId
                    );


            return diaryCount >=
                    letter.getUnlockValue();

        }



        return false;

    }


}
