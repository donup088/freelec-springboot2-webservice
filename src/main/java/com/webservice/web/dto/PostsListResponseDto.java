package com.webservice.web.dto;

import com.webservice.web.domain.posts.Posts;
import lombok.Getter;

@Getter
public class PostsListResponseDto {
    private Long id;
    private String title;
    private String content;
    private String author;

    public PostsListResponseDto(Posts posts) {
        this.id = posts.getId();
        this.title = posts.getTitle();
        this.content = posts.getContent();
        this.author = posts.getAuthor();
    }
}
