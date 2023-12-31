package com.cooksys.group01.services.impl;

import com.cooksys.group01.dtos.*;
import com.cooksys.group01.entities.Tweet;
import com.cooksys.group01.entities.User;
import com.cooksys.group01.entities.embeddable.Credentials;
import com.cooksys.group01.exceptions.BadRequestException;
import com.cooksys.group01.exceptions.NotAuthorizedException;
import com.cooksys.group01.exceptions.NotFoundException;
import com.cooksys.group01.mappers.TweetMapper;
import com.cooksys.group01.mappers.UserMapper;
import com.cooksys.group01.repositories.UserRepository;
import com.cooksys.group01.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TweetMapper tweetMapper;

    private final List<Character> allowedCharacters = new ArrayList<>(List.of('A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '{', '}'));

    @Override
    public List<UserRespDTO> getActiveUsers() {
        List<User> users = userRepository.findAllByDeletedFalse();
        List<UserRespDTO> usersToReturn = new ArrayList<>();
        for (User user : users) {
            UserRespDTO tempUser = userMapper.entityToDTO(user);
            tempUser.setUsername(user.getCredentials().getUsername());
            usersToReturn.add(tempUser);
        }
        return usersToReturn;
    }

    @Override
    public UserRespDTO getUser(String username) {
        Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opUser.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "'");
        UserRespDTO userDTO = userMapper.entityToDTO(opUser.get());
        userDTO.setUsername(opUser.get().getCredentials().getUsername());
        return userDTO;
    }

    @Override
    public List<TweetRespDTO> getUserTweets(String username) {
        Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opUser.isEmpty())
            throw new NotFoundException("Unable To Find User With Username " + username);
        List<Tweet> tweets = new ArrayList<>(opUser.get().getTweets());
        List<TweetRespDTO> tweetDTOs = new ArrayList<>();
        for(Tweet tweet : tweets)
            if(!tweet.isDeleted()) {
                TweetRespDTO tweetDTO = tweetMapper.entityToDTO(tweet);
                tweetDTO.getAuthor().setUsername(tweet.getAuthor().getCredentials().getUsername());
                tweetDTOs.add(tweetDTO);
            }
        return tweetDTOs;
    }

    @Override
    public List<UserRespDTO> getFollowers(String username) {
        Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opUser.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "'");
        User user = opUser.get();
        List<UserRespDTO> followers = new ArrayList<>();
        for (User follower : user.getFollowers())
            if (!follower.isDeleted()) {
                UserRespDTO tempUser = userMapper.entityToDTO(follower);
                tempUser.setUsername(follower.getCredentials().getUsername());
                followers.add(tempUser);
            }
        return followers;
    }

    @Override
    public List<UserRespDTO> getFollowing(String username) {
        Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opUser.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "'");
        User user = opUser.get();
        List<UserRespDTO> followings = new ArrayList<>();
        for (User following : user.getFollowing())
            if (!following.isDeleted()) {
                UserRespDTO tempUser = userMapper.entityToDTO(following);
                tempUser.setUsername(following.getCredentials().getUsername());
                followings.add(tempUser);
            }
        return followings;
    }

    @Override
    public List<TweetRespDTO> getMentions(String username) {
        // TODO - Ensure the tweets are in reverse chronological order!
        Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opUser.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "'");
        User user = opUser.get();
        List<TweetRespDTO> mentionedTweets = new ArrayList<>();
        for (Tweet t : user.getMentionedTweets())
            if (!t.isDeleted()) {
                TweetRespDTO tempTweet = tweetMapper.entityToDTO(t);
                tempTweet.getAuthor().setUsername(t.getAuthor().getCredentials().getUsername());
                mentionedTweets.add(tempTweet);
            }
        return mentionedTweets;
    }

    @Override
    public List<TweetRespDTO> getFeed(String username) {
        //TODO: Will need to check reply to and repost of tweets etc, Unfinished, still getting null usernames
        Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opUser.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "'");
        User user = opUser.get();
        List<TweetRespDTO> tweets = new ArrayList<>();
        for (Tweet userTweet : user.getTweets())
            if (!userTweet.isDeleted()) {
                TweetRespDTO tempTweet = tweetMapper.entityToDTO(userTweet);
                tempTweet.getAuthor().setUsername(userTweet.getAuthor().getCredentials().getUsername());
                tweets.add(tempTweet);
            }
        List<User> followings = user.getFollowing();
        for (User following : followings) {
            for (Tweet tweet : following.getTweets()) {
                if (!tweet.isDeleted()) {
                    TweetRespDTO tempTweet = tweetMapper.entityToDTO(tweet);
                    tempTweet.getAuthor().setUsername(tweet.getAuthor().getCredentials().getUsername());
                    tweets.add(tempTweet);
                }
                for (Tweet reply : tweet.getReplyThread()) {
                    if (reply != null && !reply.isDeleted()) {
                        TweetRespDTO tempReply = tweetMapper.entityToDTO(reply);
                        tempReply.getAuthor().setUsername(reply.getAuthor().getCredentials().getUsername());
                        tweets.add(tempReply);
                    }
                }
                for (Tweet repost : tweet.getRepostThread()) {
                    if (repost != null && !repost.isDeleted()) {
                        TweetRespDTO tempRepost = tweetMapper.entityToDTO(repost);
                        tempRepost.getAuthor().setUsername(repost.getAuthor().getCredentials().getUsername());
                        tweets.add(tempRepost);
                    }
                }
            }
        }
        return tweets;
    }

    @Override
    public UserRespDTO updateUser(String username, UserReqDTO user) {
        /* Checking for null profile or credentials. Then for null username or password. Then ensuring
        username within request body matches username in URL */
        if (user.getCredentials() == null || user.getProfile() == null)
            throw new NotAuthorizedException("Must Provide Profile And Credentials!");
        if (user.getCredentials().getUsername() == null || user.getCredentials().getPassword() == null)
            throw new NotAuthorizedException("Must Provide Credentials!");
        if (!username.equals(user.getCredentials().getUsername()))
            throw new NotAuthorizedException("Invalid Username");
        /* Find User in DB based on URL username and request body password, if
        empty, unable to find, throw bad request. Otherwise, User theUser equals the user we found */
        Optional<User> opUser = userRepository.
                findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(
                        username, user.getCredentials().getPassword());
        if (opUser.isEmpty())
            throw new BadRequestException("Invalid Credentials!");
        User theUser = opUser.get();
        /* Null checks necessary to avoid 500 level errors from null pointer exceptions, setting necessary
        fields based on what is present/not null in request body */
        if (user.getProfile() != null) {
            ProfileDTO profile = user.getProfile();
            if (profile.getEmail() != null)
                theUser.getProfile().setEmail(profile.getEmail());
            if (profile.getPhone() != null)
                theUser.getProfile().setPhone(profile.getPhone());
            if (profile.getFirstName() != null)
                theUser.getProfile().setFirstName(profile.getFirstName());
            if (profile.getLastName() != null)
                theUser.getProfile().setLastName(profile.getLastName());
        }
        /* This charade is done to send a userRespDTO that does NOT have a null username, save entity to DB */
        UserRespDTO userRespDTO = userMapper.entityToDTO(theUser);
        userRespDTO.setUsername(theUser.getCredentials().getUsername());
        userRepository.save(theUser);
        return userRespDTO;
    }

    @Override
    public UserRespDTO createUser(UserReqDTO user) {
        if (user.getCredentials() == null || user.getProfile() == null)
            throw new BadRequestException("Must Include Email, Phone, First Name, Last Name, Password, and Username");
        CredentialsDTO credentials = user.getCredentials();
        ProfileDTO profile = user.getProfile();
        if (profile.getEmail() == null || profile.getPhone() == null || profile.getFirstName() == null
                || profile.getLastName() == null || credentials.getPassword() == null
                || credentials.getUsername() == null)
            throw new BadRequestException("Must Include Email, Phone, First Name, Last Name, Password, and Username");
        String username = credentials.getUsername();
        for (int i = 0; i < username.length(); i++)
            if (!allowedCharacters.contains(username.toUpperCase().charAt(i)))
                throw new BadRequestException("Username Must Not Contain Special Characters");
        for (User tempUser : userRepository.findAll()) {
            if (tempUser.getCredentials().getPassword().equals(credentials.getPassword())
                    && tempUser.getCredentials().getUsername().equals(credentials.getUsername())) {
                Optional<User> deletedUser = userRepository.
                        findByCredentialsUsernameAndDeletedTrue(
                                credentials.getUsername());
                if (deletedUser.isPresent()) {
                    User restoredUser = deletedUser.get();
                    restoredUser.setDeleted(false);
                    userRepository.save(restoredUser);
                    UserRespDTO restoredUserDTO = userMapper.entityToDTO(restoredUser);
                    restoredUserDTO.setUsername(restoredUser.getCredentials().getUsername());
                    return restoredUserDTO;
                }
            }
            if (tempUser.getCredentials().getUsername().equals(credentials.getUsername()))
                throw new BadRequestException("Username '" + credentials.getUsername() + "' Already Exists!");
        }
        UserRespDTO userResp = userMapper.entityToDTO(userRepository.save(userMapper.dtoToEntity(user)));
        userResp.setUsername(credentials.getUsername());
        return userResp;
    }

    @Override
    public void followUser(String username, Credentials credentials) {
        Optional<User> opToBeFollowed = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opToBeFollowed.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "'");
        User toBeFollowed = opToBeFollowed.get();

        Optional<User> opUser = userRepository
                .findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(
                        credentials.getUsername(), credentials.getPassword());
        if (opUser.isEmpty())
            throw new NotAuthorizedException("Not Authorized: Could Not Verify Credentials");

        User user = opUser.get();
        if (user.getFollowing().contains(toBeFollowed))
            throw new BadRequestException("Already following " + username + "!");

        user.addFollowing(toBeFollowed);
        userRepository.saveAndFlush(user);
    }

    @Override
    public void unfollowUser(String username, Credentials credentials) {
        Optional<User> opToUnfollow = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
        if (opToUnfollow.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "'");
        User toUnfollow = opToUnfollow.get();

        Optional<User> opUser = userRepository
                .findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(
                        credentials.getUsername(), credentials.getPassword());
        if (opUser.isEmpty())
            throw new NotAuthorizedException("Not Authorized: Could Not Verify Credentials");

        User user = opUser.get();

        if (!user.getFollowing().contains(toUnfollow))
            throw new BadRequestException("You currently do not follow " + username + "!");

        user.removeFollowing(toUnfollow);
        userRepository.saveAndFlush(user);
    }

    @Override
    public UserRespDTO deleteUser(String username, CredentialsDTO credentials) {
        if (credentials.getUsername() == null || credentials.getPassword() == null)
            throw new NotAuthorizedException("Invalid Credentials!");
        if (!credentials.getUsername().equals(username))
            throw new NotAuthorizedException("Invalid Username");
        Optional<User> opUser = userRepository
                .findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(
                        username, credentials.getPassword());
        if (opUser.isEmpty())
            throw new NotFoundException("Unable To Find Username '" + username + "' With Provided Credentials");
        User user = opUser.get();
        user.setDeleted(true);
        userRepository.save(user);
        UserRespDTO userRespDTO = userMapper.entityToDTO(user);
        userRespDTO.setUsername(user.getCredentials().getUsername());
        return userRespDTO;
    }

}
