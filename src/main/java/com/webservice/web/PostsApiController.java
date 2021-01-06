package com.webservice.web;

import com.webservice.web.dto.PostsResponseDto;
import com.webservice.web.dto.PostsSaveRequestDto;
import com.webservice.web.dto.PostsUpdateRequestDto;
import com.webservice.web.service.PostsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class PostsApiController {
    private final PostsService postsService;

    @PostMapping("/api/v1/posts")
    public Long save(@RequestBody PostsSaveRequestDto requestDto){
        return postsService.save(requestDto);
    }

    @PutMapping("/api/v1/posts/{id}")
    public Long update(@PathVariable("id") Long id,@RequestBody PostsUpdateRequestDto requestDto) throws IllegalAccessException {
        return postsService.update(id,requestDto);
    }

    @GetMapping("/api/v1/posts/{id}")
    public PostsResponseDto findById(@PathVariable("id") Long id) throws IllegalAccessException {
        return postsService.findById(id);
    }
}
