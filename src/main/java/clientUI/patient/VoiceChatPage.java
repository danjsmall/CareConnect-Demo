package clientUI.patient;

import clientUI.FXComponents;
import clientUI.MainPage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class VoiceChatPage {
    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final MainPage parent;
    private final String username;
    private final int level;
    private final VoiceChatService voiceChatService;

    private VBox messagesBox;
    private Label statusLabel;
    private TextField inputField;
    private Button micButton;
    private Button sendButton;
    private ScrollPane messagesPane;
    private boolean listening;

    public VoiceChatPage(MainPage parent, String username, int level) {
        this.parent = parent;
        this.username = username;
        this.level = level;
        this.voiceChatService = new VoiceChatService(username);
    }

    public void start(Stage stage) {
        stage.setTitle("Voice Chat Assistant");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("voice-page");
        root.setTop(buildHeader(stage));
        root.setCenter(buildContent());
        root.setBottom(buildComposer(stage));

        Scene scene = new Scene(root, 1024, 768);
        var stylesheet = getClass().getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.show();

        addAssistantMessage(voiceChatService.createWelcomeMessage());
    }

    private VBox buildHeader(Stage stage) {
        VBox header = FXComponents.paddedCard(new Insets(20), "hero-card", "voice-hero-card");
        header.setSpacing(16);

        Label eyebrow = new Label("ASSISTANT");
        eyebrow.getStyleClass().add("hero-kicker");

        Label title = new Label("Voice Assistant");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label("Ask about appointments, medicines, vaccines, GP details, or your health summary.");
        subtitle.getStyleClass().add("hero-body");
        subtitle.setWrapText(true);

        VBox titleBlock = new VBox(4, eyebrow, title, subtitle);
        titleBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        Button backButton = FXComponents.secondaryButton("Back to Dashboard");
        backButton.setOnAction(e -> parent.start(stage));

        HBox topRow = new HBox(12, titleBlock, backButton);
        topRow.setAlignment(Pos.TOP_LEFT);

        statusLabel = new Label("Microphone idle");
        statusLabel.getStyleClass().addAll("status-pill", "status-pill-neutral", "voice-status-pill");

        Label sessionLabel = new Label("Signed in as " + username + " | Access level " + level + " | Simulated microphone mode");
        sessionLabel.getStyleClass().add("hero-note");

        HBox metaRow = new HBox(10,
            FXComponents.statusPill("Local patient record mode", "status-pill-success"),
            FXComponents.statusPill("Follow-up prompts enabled", "status-pill-neutral"),
            statusLabel
        );
        metaRow.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(topRow, sessionLabel, metaRow);
        return header;
    }

    private HBox buildContent() {
        HBox content = new HBox(18);
        content.setPadding(new Insets(0, 18, 0, 18));

        VBox chatPanel = FXComponents.paddedCard(new Insets(18), "detail-card", "voice-chat-panel");
        chatPanel.setSpacing(14);
        HBox.setHgrow(chatPanel, Priority.ALWAYS);

        Label chatTitle = new Label("Conversation");
        chatTitle.getStyleClass().add("section-heading");

        Label chatBody = new Label("Use the quick prompts or type a transcript to ask about your local health record.");
        chatBody.getStyleClass().add("section-copy");
        chatBody.setWrapText(true);

        messagesBox = new VBox(12);
        messagesBox.setFillWidth(true);

        messagesPane = new ScrollPane(messagesBox);
        messagesPane.setFitToWidth(true);
        messagesPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesPane.getStyleClass().addAll("scroll-pane", "voice-messages-pane");
        VBox.setVgrow(messagesPane, Priority.ALWAYS);

        FlowPane quickActions = new FlowPane();
        quickActions.setHgap(10);
        quickActions.setVgap(10);
        quickActions.getChildren().addAll(
            createQuickAction("Health summary"),
            createQuickAction("Next appointment"),
            createQuickAction("Upcoming appointments"),
            createQuickAction("Full prescription list"),
            createQuickAction("Latest vaccine"),
            createQuickAction("GP contact details"),
            createQuickAction("What should I ask next?")
        );

        chatPanel.getChildren().addAll(chatTitle, chatBody, messagesPane, quickActions);

        VBox infoPanel = new VBox(14);
        infoPanel.setPrefWidth(320);
        infoPanel.getChildren().addAll(
            createInfoCard("Patient Snapshot", voiceChatService.buildPatientSnapshot()),
            createInfoCard("Suggested Prompts",
                "What is my next appointment?\n\n" +
                "List my upcoming appointments.\n\n" +
                "Give me my full prescription list.\n\n" +
                "Do I have any recorded conditions?\n\n" +
                "Show my GP contact details.\n\n" +
                "Give me a health summary."),
            createInfoCard("Voice Flow",
                "Use Start Mic to simulate listening.\n\n" +
                "Type what the patient said, then press Send.\n\n" +
                "You can follow up with more details, full list, or what next."),
            createInfoCard("Safety Note",
                "This tool reads local patient records for support and quick lookup.\n\n" +
                "For urgent health concerns, contact your GP, NHS 111, or 999.")
        );

        content.getChildren().addAll(chatPanel, infoPanel);
        return content;
    }

    private VBox buildComposer(Stage stage) {
        VBox wrapper = new VBox(12);
        wrapper.setPadding(new Insets(14, 18, 20, 18));

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        micButton = new Button("Start Mic");
        micButton.getStyleClass().add("btn-primary");
        micButton.setOnAction(e -> {
            listening = !listening;
            micButton.setText(listening ? "Stop Mic" : "Start Mic");
            statusLabel.getStyleClass().removeAll("status-pill-neutral", "status-pill-success", "status-pill-warning");
            statusLabel.getStyleClass().addAll("status-pill", listening ? "status-pill-success" : "status-pill-neutral");
            statusLabel.setText(listening
                ? "Listening for patient speech... type the transcript below, then press Send."
                : "Microphone idle");
        });

        inputField = new TextField();
        inputField.setPromptText("Type a voice request, for example: What is my next appointment?");
        inputField.getStyleClass().addAll("app-field", "voice-input-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.textProperty().addListener((obs, oldValue, newValue) -> updateComposerState());
        inputField.setOnAction(e -> sendMessage());

        sendButton = new Button("Send");
        sendButton.getStyleClass().add("btn-primary");
        sendButton.setOnAction(e -> sendMessage());

        Button clearButton = FXComponents.secondaryButton("Clear Chat");
        clearButton.setOnAction(e -> clearConversation());

        Button backButton = FXComponents.secondaryButton("Back");
        backButton.setOnAction(e -> parent.start(stage));

        controls.getChildren().addAll(micButton, inputField, sendButton, clearButton, backButton);

        Label footerNote = new Label("Current mode uses typed transcripts for the microphone flow. Replies are generated from the local patient record cache.");
        footerNote.getStyleClass().add("helper-text");
        footerNote.setWrapText(true);

        wrapper.getChildren().addAll(new Separator(), controls, footerNote);
        updateComposerState();
        return wrapper;
    }

    private Button createQuickAction(String prompt) {
        Button button = new Button(prompt);
        button.getStyleClass().add("pill-btn");
        button.setOnAction(e -> {
            inputField.setText(prompt);
            sendMessage();
        });
        return button;
    }

    private VBox createInfoCard(String title, String body) {
        VBox card = FXComponents.paddedCard(new Insets(16), "detail-card", "voice-info-card");
        card.setSpacing(10);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");

        Label bodyLabel = new Label(body);
        bodyLabel.setWrapText(true);
        bodyLabel.getStyleClass().add("body-text");

        card.getChildren().addAll(titleLabel, bodyLabel);
        return card;
    }

    private void sendMessage() {
        String prompt = inputField.getText().trim();
        if (prompt.isEmpty()) {
            setStatus("Type a request before sending.", "status-pill-warning");
            return;
        }

        addUserMessage(prompt);
        inputField.clear();
        listening = false;
        micButton.setText("Start Mic");
        setBusyState(true);

        String reply = voiceChatService.generateReply(prompt);
        addAssistantMessage(reply);

        setBusyState(false);
        setStatus("Response ready. You can ask a follow-up like more details or what should I ask next.", "status-pill-neutral");
        inputField.requestFocus();
    }

    private void clearConversation() {
        messagesBox.getChildren().clear();
        listening = false;
        micButton.setText("Start Mic");
        addAssistantMessage(voiceChatService.createWelcomeMessage());
        inputField.clear();
        updateComposerState();
        setStatus("Conversation cleared. Starting a fresh voice chat session.", "status-pill-neutral");
    }

    private void addUserMessage(String message) {
        messagesBox.getChildren().add(createBubble("You", message, true));
        scrollToBottom();
    }

    private void addAssistantMessage(String message) {
        messagesBox.getChildren().add(createBubble("Assistant", message, false));
        scrollToBottom();
    }

    private HBox createBubble(String speaker, String message, boolean userMessage) {
        HBox row = new HBox();
        row.setAlignment(userMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(540);
        bubble.setPadding(new Insets(12));
        bubble.getStyleClass().addAll("voice-bubble", userMessage ? "voice-bubble-user" : "voice-bubble-assistant");

        Label speakerLabel = new Label(speaker + " • " + LocalTime.now().format(MESSAGE_TIME));
        speakerLabel.getStyleClass().add("helper-text");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("body-text");

        bubble.getChildren().addAll(speakerLabel, messageLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (userMessage) {
            row.getChildren().addAll(spacer, bubble);
        } else {
            row.getChildren().addAll(bubble, spacer);
        }
        return row;
    }

    private void scrollToBottom() {
        if (messagesPane == null) {
            return;
        }
        Platform.runLater(() -> {
            messagesPane.layout();
            messagesPane.setVvalue(1.0);
        });
    }

    private void setBusyState(boolean busy) {
        sendButton.setDisable(busy || inputField.getText().trim().isEmpty());
        inputField.setDisable(busy);
        micButton.setDisable(busy);
        if (busy) {
            setStatus("Assistant is reviewing the local record...", "status-pill-warning");
        }
    }

    private void updateComposerState() {
        if (sendButton != null && inputField != null) {
            sendButton.setDisable(inputField.getText().trim().isEmpty());
        }
    }

    private void setStatus(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-pill-neutral", "status-pill-success", "status-pill-warning", "status-pill-danger");
        statusLabel.getStyleClass().addAll("status-pill", styleClass);
    }
}
