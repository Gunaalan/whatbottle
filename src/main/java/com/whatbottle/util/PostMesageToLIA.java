package com.whatbottle.util;

import com.lithium.mineraloil.api.lia.LIAAPIConnection;
import com.lithium.mineraloil.api.lia.api.models.Board;
import com.lithium.mineraloil.api.lia.api.models.Message;
import com.lithium.mineraloil.api.lia.api.models.Parent;
import com.lithium.mineraloil.api.lia.api.models.User;
import com.lithium.mineraloil.api.lia.api.v1.BoardV1API;
import com.lithium.mineraloil.api.lia.api.v1.CategoryV1API;
import com.lithium.mineraloil.api.lia.api.v1.models.BoardV1Response;
import com.lithium.mineraloil.api.lia.api.v1.models.CategoryV1Response;
import com.lithium.mineraloil.api.lia.api.v2.BoardV2API;
import com.lithium.mineraloil.api.lia.api.v2.models.BoardV2;
import com.lithium.mineraloil.api.lia.api.v2.models.Category;
import com.lithium.mineraloil.api.lia.api.v2.models.MessageV2Response;
import com.lithium.mineraloil.api.rest.RestAPIException;
import com.whatbottle.data.Requests.MessageRequest;
import com.whatbottle.data.Requests.WhatsAppMessage;
import com.whatbottle.data.pojos.ConversationStyles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostMesageToLIA {
    private static LIAAPIConnection liaapiConnection;
    private static BoardV2API boardV2API ;//new BoardV2API(liaapiConnection);
    private static Board board;

    @Value("${communityUrl}")
    private String communityUrl;

    @Value("${communityName}")
    private String communityName;

    @Value("${boardId}")
    private String boardId;

    @Value("${boardTitle}")
    private String boardTitle;

    @Value("${category}")
    private String liaCategory;


    /*
     * Sample on how to use, need to remove later
     * */
    private void postAMessageToCommunity(MessageRequest messageRequest) {
        User user = LiaApiConnector.getDefaultUser();
        this.liaapiConnection = LiaApiConnector.getLIAAPIConnectionV1(user, communityUrl, 8080, communityName);
        Board board = createLIABoard(this.liaapiConnection, boardId, boardTitle, liaCategory, ConversationStyles.forum);
        Message message = Message.builder()
                .subject(messageRequest.getTopicName())
                .body(messageRequest.getMessage().toString())
                .build();
        postMessage(liaapiConnection, board, message);
    }

    private Board createBoard(BoardV2 boardV2) {
        Board board = Board.builder()
                .type(boardV2.getType())
                .viewHref(boardV2.getViewHref())
                .href(boardV2.getHref())
                .boardName(boardV2.getTitle())
                .boardType("boards")
                .boardId(boardV2.getId())
                .id(boardV2.getId())
                .build();
        return board;
    }

    private Board createLIABoard(LIAAPIConnection liaapiConnection, String boardId, String boardTitle, String liaCategory, ConversationStyles conversationStyles) {
        Category category = Category.builder().id(liaCategory).build();
        createCategory(liaapiConnection, liaCategory, liaCategory);
        BoardV2 board = BoardV2.builder()
                .type("board")
                .parentCategory(category)
                .id(boardId)
                .conversation_style(conversationStyles.toString())
                .title(boardTitle)
                .build();

        return createBoard(new BoardV2API(liaapiConnection).createBoard(board));
    }

    private CategoryV1Response createCategory(LIAAPIConnection connection, String categoryId, String categoryTitle) {
        CategoryV1Response categoryV1Response = null;
        try {
            categoryV1Response = new CategoryV1API(connection).createCategory(categoryId, categoryTitle);
        } catch (RestAPIException restAPIException) {
            log.info("Category with already available with ID " + categoryId);
        }
        return categoryV1Response;
    }

    private BoardV1Response postMessage(LIAAPIConnection liaapiConnection, Board board, Message message) {

        BoardV1Response messagePostResponse = new BoardV1API(liaapiConnection)
                .postMessage(board, message);
        return messagePostResponse;
    }

    public MessageRequest postToCommunityWithNewTopic(String message) {
        MessageRequest messageRequest = new MessageRequest();
        messageRequest.setTopicName(message);
        messageRequest.setMessage(message);
        postAMessageToCommunity(messageRequest);
        return messageRequest;
    }

    public MessageV2Response replyToTopic(WhatsAppMessage whatsAppMessage){
        Message message = convertToMessage(whatsAppMessage);
        User user = LiaApiConnector.getDefaultUser();

        this.liaapiConnection = LiaApiConnector.getLIAAPIConnectionV1(user, communityUrl, -1, communityName);
        BoardV2 boardv2 = new BoardV2API(liaapiConnection).getBoard(whatsAppMessage.getBoardName());
        MessageReply messageReplyV2API = new MessageReply(liaapiConnection);
        Parent parent  = Parent.builder().id(whatsAppMessage.getId()).build();
        MessageV2Response messageV2Response =null;
        try {
            messageV2Response = messageReplyV2API.postMessageReply(createBoard(boardv2), message, user, parent);
        }catch (Exception e){
            log.error("Reply cannot be commpleted: "+e.getMessage());
            e.printStackTrace();
        }
        return messageV2Response;
    }

    private Message convertToMessage(WhatsAppMessage whatsAppMessage) {
        Message message = Message.builder().
                body(whatsAppMessage.getMessageBody()).
                author(whatsAppMessage.getAuthor()).
                build();
        return  message;
    }

}
