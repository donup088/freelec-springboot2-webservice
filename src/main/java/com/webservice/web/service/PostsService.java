package com.webservice.web.service;

import com.webservice.web.domain.posts.Posts;
import com.webservice.web.domain.posts.PostsRepository;
import com.webservice.web.dto.PostsListResponseDto;
import com.webservice.web.dto.PostsResponseDto;
import com.webservice.web.dto.PostsSaveRequestDto;
import com.webservice.web.dto.PostsUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class PostsService {

    private final PostsRepository postsRepository;

    public Long save(PostsSaveRequestDto requestDto) {
        return postsRepository.save(requestDto.toEntity()).getId();
    }

    public Long update(Long id, PostsUpdateRequestDto requestDto) throws IllegalAccessException {
        Posts posts = postsRepository.findById(id).orElseThrow(() -> new IllegalAccessException("해당 게시글이 없습니다. id= " + id));
        posts.update(requestDto.getTitle(), requestDto.getContent());
        return id;
    }

    public PostsResponseDto findById(Long id) throws IllegalAccessException {
        Posts posts = postsRepository.findById(id).orElseThrow(() -> new IllegalAccessException("해당 게시글이 없습니다. id= " + id));

        return new PostsResponseDto(posts);
    }
    @Transactional(readOnly = true)
    public List<PostsListResponseDto> findAllDesc(){
        return postsRepository.findAllDesc().stream()
                .map(PostsListResponseDto::new)
                .collect(Collectors.toList());
    }

    public void delete(Long id) throws IllegalAccessException {
        Posts posts = postsRepository.findById(id).orElseThrow(() -> new IllegalAccessException("해당 게시글이 없습니다. id= " + id));

        postsRepository.delete(posts);
    }
}
