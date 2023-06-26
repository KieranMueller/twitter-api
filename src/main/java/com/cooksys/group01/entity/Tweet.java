package com.cooksys.group01.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "author")
    private User author;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, insertable = false)
    private Timestamp posted;

    private boolean deleted;

    private String content;

    @ManyToMany(mappedBy = "likedTweets")
    private Set<User> likes;

    @ManyToMany(mappedBy = "mentionedTweets")
    private Set<User> mentions;

    @ManyToMany
    @JoinTable(
            name = "tweet_hashtags",
            joinColumns = @JoinColumn(name = "tweet_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id"))
    private Set<Hashtag> hashtags;

    @OneToMany(mappedBy = "inReplyTo")
    private Set<Tweet> replyThread;

    @ManyToOne
    @JoinColumn(name = "inReplyTo")
    private Tweet inReplyTo;

    @OneToMany(mappedBy = "repostOf")
    private Set<Tweet> repostThread;

    @ManyToOne
    @JoinColumn(name = "repostOf")
    private Tweet repostOf;
}