package com.cooksys.group01.services.impl;

import com.cooksys.group01.dtos.*;
import com.cooksys.group01.entities.Hashtag;
import com.cooksys.group01.entities.Tweet;
import com.cooksys.group01.entities.User;
import com.cooksys.group01.exceptions.BadRequestException;
import com.cooksys.group01.exceptions.NotAuthorizedException;
import com.cooksys.group01.exceptions.NotFoundException;
import com.cooksys.group01.mappers.HashtagMapper;
import com.cooksys.group01.mappers.TweetMapper;
import com.cooksys.group01.mappers.UserMapper;
import com.cooksys.group01.repositories.HashtagRepository;
import com.cooksys.group01.repositories.TweetRepository;
import com.cooksys.group01.repositories.UserRepository;
import com.cooksys.group01.services.TweetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TweetServiceImpl implements TweetService {

	private final TweetRepository tweetRepository;
	private final UserRepository userRepository;
	private final HashtagRepository hashtagRepository;
	private final TweetMapper tweetMapper;
    private final UserMapper userMapper;
    private final HashtagMapper hashtagMapper;
  
    List<Character> allowedCharacters = new ArrayList<>(List.of('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1',
            '2', '3', '4', '5', '6', '7', '8', '9', '{', '}'));

	@Override
	public List<TweetRespDTO> getRepliesById(Long id) {
		Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
		if (opTweet.isEmpty())
			throw new NotFoundException("Unable To Find Tweet With ID " + id);
        List<Tweet> replies = opTweet.get().getReplyThread();
        List<TweetRespDTO> replyDTOs = new ArrayList<>();
        for(Tweet reply : replies)
            if(!reply.isDeleted()) {
                TweetRespDTO replyDTO = tweetMapper.entityToDTO(reply);
                replyDTO.getInReplyTo().getAuthor().setUsername(reply.getInReplyTo().getAuthor().getCredentials().getUsername());
                replyDTO.getAuthor().setUsername(reply.getAuthor().getCredentials().getUsername());
                replyDTOs.add(replyDTO);
            }
        return replyDTOs;
	}

	@Override
	public TweetRespDTO repostById(Long id, CredentialsDTO credentials) {
		if (credentials.getUsername() == null || credentials.getPassword() == null)
			throw new BadRequestException("Could not verify credentials");
		Optional<User> opUser = userRepository.findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(
				credentials.getUsername(), credentials.getPassword());
		if (opUser.isEmpty())
			throw new BadRequestException("Could not verify credentials");
		Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
		if (opTweet.isEmpty())
			throw new NotFoundException("Unable to find tweet with ID " + id);
		Tweet tweet = opTweet.get();
		User user = opUser.get();
		Tweet repost = tweetRepository.save(new Tweet(null, user, null, false, null, null, null, null, null, null, null, tweet));
		TweetRespDTO repostDTO = tweetMapper.entityToDTO(repost);
		repostDTO.getAuthor().setUsername(repost.getAuthor().getCredentials().getUsername());
		repostDTO.getRepostOf().getAuthor().setUsername(repost.getRepostOf().getAuthor().getCredentials().getUsername());
		return repostDTO;
	}

    @Override
    public TweetRespDTO replyToTweet(Long id, TweetReqDTO tweetReqDTO) {
        Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
        if(opTweet.isEmpty())
            throw new NotFoundException("Unable To Find Tweet With ID " + id);
        if(tweetReqDTO.getCredentials() == null || tweetReqDTO.getContent() == null)
            throw new BadRequestException("Invalid Credentials");
        if(tweetReqDTO.getCredentials().getPassword() == null || tweetReqDTO.getCredentials().getUsername() == null)
            throw new BadRequestException("Invalid Credentials");
        CredentialsDTO credentials = tweetReqDTO.getCredentials();
        Optional<User> opUser = userRepository
                .findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(
                credentials.getUsername(), credentials.getPassword());
        if(opUser.isEmpty())
            throw new NotAuthorizedException("Invalid Credentials");
        // Remember to scan the tweet for mentions and hashtags
        Tweet tweet = opTweet.get();
        User user = opUser.get();
        Tweet reply = tweetMapper.dtoToEntity(tweetReqDTO);
        reply.setInReplyTo(tweet);
        reply.setAuthor(user);
        Tweet savedReply = tweetRepository.save(reply);
        tweet.addReply(savedReply);
        tweetRepository.save(tweet);
        TweetRespDTO replyDTO = tweetMapper.entityToDTO(savedReply);
        replyDTO.setAuthor(userMapper.entityToDTO(user));
        replyDTO.getAuthor().setUsername(user.getCredentials().getUsername());
        replyDTO.getInReplyTo().getAuthor().setUsername(tweet.getAuthor().getCredentials().getUsername());
        return replyDTO;
    }

    @Override
    public List<TweetRespDTO> getAllTweets() {
        List<Tweet> allTweets = tweetRepository.findByDeletedFalseOrderByPostedDesc();
        List<TweetRespDTO> allTweetsDTO = new ArrayList<>();
        for(Tweet tweet : allTweets) {
            TweetRespDTO tweetDTO = tweetMapper.entityToDTO(tweet);
            tweetDTO.getAuthor().setUsername(tweet.getAuthor().getCredentials().getUsername());
            allTweetsDTO.add(tweetDTO);
        }
        return allTweetsDTO;
    }

    @Override
    public TweetRespDTO getTweetById(Long id) {
        Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
        if(opTweet.isEmpty())
            throw new NotFoundException("Unable To Find Tweet With ID " + id);
        Tweet tweet = opTweet.get();
        TweetRespDTO tweetDTO = tweetMapper.entityToDTO(tweet);
        tweetDTO.getAuthor().setUsername(tweet.getAuthor().getCredentials().getUsername());
        return tweetDTO;
    }

    @Override
    public List<UserRespDTO> getMentionsById(Long id) {
        Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
        if(opTweet.isEmpty()) {
            throw new NotFoundException("Unable To Find Tweet With ID " + id + "!");
        }
        Tweet tweet = opTweet.get();

        List<User> users = new ArrayList<>();

        for(User possibleUser : tweet.getMentionedUsers()) {
            String username = possibleUser.getCredentials().getUsername();
            Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(username);
            if(opUser.isPresent()) {
                User user = opUser.get();
                users.add(user);
            }
        }

        List<UserRespDTO> userRespDTOs = new ArrayList<>();
        for(User user : users) {
            UserRespDTO userDTO = userMapper.entityToDTO(user);
            userDTO.setUsername(user.getCredentials().getUsername());
            userRespDTOs.add(userDTO);
        }

        return userRespDTOs;
    }

    @Override
    public List<TweetRespDTO> getRepostsById(Long id) {
        Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
        if(opTweet.isEmpty()) {
            throw new NotFoundException("Unable To Find Tweet With ID " + id + "!");
        }
        Tweet tweet = opTweet.get();

        List<Tweet> reposts = new ArrayList<>();

        for(Tweet possibleRepost : tweet.getRepostThread()) {
            Optional<Tweet> opRepost = tweetRepository.findByIdAndDeletedFalse(possibleRepost.getId());
            if(opRepost.isPresent()) {
                Tweet repost = opRepost.get();
                reposts.add(repost);
            }
        }

        List<TweetRespDTO> repostDTOs = new ArrayList<>();
        for(Tweet repost : reposts) {
            TweetRespDTO repostDTO = tweetMapper.entityToDTO(repost);
            repostDTO.getAuthor().setUsername(repost.getAuthor().getCredentials().getUsername());
            repostDTOs.add(repostDTO);
        }
        return repostDTOs;
    }

    @Override
    public ContextRespDTO getContextById(Long id) {
        Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
        if(opTweet.isEmpty()) {
            throw new NotFoundException("Tweet With ID " + id + " Not Found!");
        }
        Tweet tweet = opTweet.get();

        // TARGET CONTEXT
        ContextRespDTO contextDTO = new ContextRespDTO();
        contextDTO.setTarget(tweetMapper.entityToDTO(tweet));
        contextDTO.getTarget().getAuthor().setUsername(tweet.getAuthor().getCredentials().getUsername());
        if(contextDTO.getTarget().getInReplyTo() != null) {
            contextDTO.getTarget().getInReplyTo().getAuthor().setUsername(tweet.getInReplyTo().getAuthor().getCredentials().getUsername());
        }

        // BEFORE CONTEXT
        List<Tweet> beforeList = new ArrayList<>();
        List<TweetRespDTO> beforeRespDTOs = new ArrayList<>();
        Tweet actualTweet = tweet;
        while (actualTweet.getInReplyTo() != null) {
            actualTweet = actualTweet.getInReplyTo();
            checkStatus: if (!actualTweet.isDeleted()) {
                Optional<User> opUser = userRepository.findByCredentialsUsername(actualTweet.getAuthor().getCredentials().getUsername());
                if(opUser.isEmpty()) {
                    break checkStatus;
                }
                User user = opUser.get();
                UserRespDTO userDTO = userMapper.entityToDTO(user);

                TweetRespDTO tweetRespDTO = new TweetRespDTO();
                tweetRespDTO.setId(actualTweet.getId());
                tweetRespDTO.setAuthor(userDTO);
                tweetRespDTO.getAuthor().setUsername(actualTweet.getAuthor().getCredentials().getUsername());
                tweetRespDTO.setPosted(actualTweet.getPosted());
                tweetRespDTO.setContent(actualTweet.getContent());
                beforeRespDTOs.add(tweetRespDTO);
            }
        }
        contextDTO.setBefore(beforeRespDTOs);

        // AFTER CONTEXT
        List<Tweet> afterList = new ArrayList<>();
        performBFS(tweet.getReplyThread(), afterList);
        List<TweetRespDTO> afterRespDTOs = new ArrayList<>();
        for(Tweet twt : afterList) {
            TweetRespDTO tweetRespDTO = new TweetRespDTO();
            tweetRespDTO.setId(twt.getId());
            Optional<User> opUser = userRepository.findByCredentialsUsernameAndDeletedFalse(twt.getAuthor().getCredentials().getUsername());
            if(opUser.isEmpty()) {
                break;
            }
            User user = opUser.get();
            UserRespDTO userDTO = userMapper.entityToDTO(user);
            System.out.println("user: " + user.getCredentials().getUsername());
            tweetRespDTO.setAuthor(userDTO);
            tweetRespDTO.getAuthor().setUsername(twt.getAuthor().getCredentials().getUsername());
            tweetRespDTO.setContent(twt.getContent());
            afterRespDTOs.add(tweetRespDTO);
        }

        contextDTO.setAfter(afterRespDTOs);
        return contextDTO;
    }

    @Override
    public TweetRespDTO createTweet(TweetReqDTO tweet) {
        if(tweet.getContent() == null || tweet.getCredentials() == null)
            throw new BadRequestException("Tweet Must Contain Content, Username, and Password");
        if(tweet.getContent().isBlank())
            throw new BadRequestException("Tweet Must Contain Content");
        Optional<User> opUser = userRepository.
                findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(
                        tweet.getCredentials().getUsername(), tweet.getCredentials().getPassword());
        if(opUser.isEmpty())
            throw new BadRequestException("Username/Password Combination Invalid!");
        User user = opUser.get();
        Tweet tweetEntity = tweetMapper.dtoToEntity(tweet);
        tweetEntity.setAuthor(user);
        String[] arr = tweet.getContent().split(" ");
        Set<String> usernamesMentioned = new HashSet<>();
        Set<String> hashtags = new HashSet<>();
        for(String word : arr) {
            String toAdd = "";
            int i = 0;
            if(word.startsWith("@")) {
                i++;
                while(i < word.length() && allowedCharacters.contains(word.toUpperCase().charAt(i))) {
                    toAdd += word.charAt(i);
                    i++;
                }
                usernamesMentioned.add(toAdd);
            } else if(word.startsWith("#")) {
                toAdd += "#";
                i++;
                while(i < word.length() && allowedCharacters.contains(word.toUpperCase().charAt(i))) {
                    toAdd += word.charAt(i);
                    i++;
                }
                hashtags.add(toAdd);
            }
        }
        List<Hashtag> savedTags = new ArrayList<>();
        List<String> existingLabels = new ArrayList<>(hashtagRepository.getAllLabels());
        for(String hashtag : hashtags) {
            if(!existingLabels.contains(hashtag))
                savedTags.add(hashtagRepository.save(new Hashtag(null, hashtag, null, null, null)));
            else {
                Optional<Hashtag> tagToUpdate = hashtagRepository.findByLabel(hashtag);
                if(tagToUpdate.isPresent()) {
                    Date date = new Date();
                    tagToUpdate.get().setLastUsed(new Timestamp(date.getTime()));
                    hashtagRepository.save(tagToUpdate.get());
                }
            }
        }
        for(String possibleUser : usernamesMentioned) {
            Optional<User> foundUser = userRepository.findByCredentialsUsernameAndDeletedFalse(possibleUser);
            // Might need to save each user after this
            foundUser.ifPresent(value -> value.addMentionedTweet(tweetEntity));
        }
        tweetEntity.setHashtags(savedTags);
        TweetRespDTO savedTweet = tweetMapper.entityToDTO(tweetRepository.save(tweetEntity));
        savedTweet.getAuthor().setUsername(user.getCredentials().getUsername());
        return savedTweet;
    }

    @Override
    public ResponseEntity<HttpStatus> likeTweet(Long id, CredentialsDTO credentials) {
        if(credentials.getPassword() == null || credentials.getUsername() == null)
            throw new NotAuthorizedException("Unable To Verify Credentials");
        Optional<User> opUser = userRepository.
                findByCredentialsUsernameAndCredentialsPasswordAndDeletedFalse(credentials.getUsername(), credentials.getPassword());
        if(opUser.isEmpty())
            throw new NotAuthorizedException("Unable To Verify Credentials");
        Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
        if(opTweet.isEmpty())
            throw new NotFoundException("Unable To Find Tweet With ID " + id);
        Tweet tweet = opTweet.get();
        User user = opUser.get();
        if(!user.getLikedTweets().contains(tweet))
            user.addLikedTweet(tweet);
        userRepository.saveAndFlush(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public TweetRespDTO deleteTweetById(Long id) {
        Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
        if(opTweet.isEmpty())
            throw new NotFoundException("Unable To Find Tweet With ID " + id);
        Tweet tweet = opTweet.get();
        TweetRespDTO tweetDTO = tweetMapper.entityToDTO(tweet);
        tweet.setDeleted(true);
        tweetRepository.save(tweet);
        tweetDTO.getAuthor().setUsername(tweet.getAuthor().getCredentials().getUsername());
        return tweetDTO;
    }

	@Override
	public List<UserRespDTO> getUsersByLikedTweet(Long id) {
		 Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
	        if(opTweet.isEmpty())
	            throw new NotFoundException("Unable To Find Tweet With ID " + id);
	        Tweet tweet = opTweet.get();
	        List<User> users = tweet.getLikes();
	        List<UserRespDTO> replyDTOs = new ArrayList<>();
	        for(User likes : users)
	            if(!likes.isDeleted()) {
	            	String userName = likes.getCredentials().getUsername();
	                UserRespDTO tempLikesDTO = userMapper.entityToDTO(likes);
	                tempLikesDTO.setUsername(userName);
	                replyDTOs.add(tempLikesDTO);
	            }
		    return replyDTOs;
	}

	@Override
	public List<HashtagDTO> getTagsByTweetId(Long id) {
		 Optional<Tweet> opTweet = tweetRepository.findByIdAndDeletedFalse(id);
	        if(opTweet.isEmpty())
	            throw new NotFoundException("Unable To Find Tweet With ID " + id);
	        Tweet tweet = opTweet.get();
	        List<Hashtag> hashtags = tweet.getHashtags();
	        for (Hashtag hash : hashtags) {
	        	hash.setLabel(hash.getLabel().replace("#", ""));
	        }
		return hashtagMapper.entitiesToDTOs(hashtags);
	}

    // HELPER FUNCTIONS
    private void performBFS(List<Tweet> replyThread, List<Tweet> tweetsAfter) {
        if (replyThread == null || replyThread.isEmpty()) {
            return;
        }

        Queue<Tweet> queue = new LinkedList<>(replyThread);
        while (!queue.isEmpty()) {
            Tweet tweet = queue.poll();
            if (!tweet.isDeleted()) {
                tweetsAfter.add(tweet);
            }
            queue.addAll(tweet.getReplyThread());
        }
    }
}
