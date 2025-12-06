import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ModernMusicPlayer extends Application {

    private MediaPlayer mediaPlayer;
    private List<File> playList = new ArrayList<>();
    private int currentIndex = -1;

    // UI 组件
    private ListView<String> playlistView;
    private Label titleLabel;   // 歌名
    private Label artistLabel;  // 歌手/状态
    private Label timeLabel;    // 时间
    private Slider volumeSlider;
    private Slider progressSlider;
    private Button btnPlay;
    private StackPane coverPane; // 封面区域

    @Override
    public void start(Stage primaryStage) {
        // --- 1. 根布局 (使用深色渐变背景) ---
        BorderPane root = new BorderPane();
        Stop[] stops = new Stop[] { new Stop(0, Color.web("#1c1c1c")), new Stop(1, Color.web("#303030")) };
        LinearGradient bgGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops);
        root.setBackground(new Background(new BackgroundFill(bgGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // --- 2. 左侧：播放列表 (半透明磨砂感) ---
        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setPrefWidth(240);
        leftPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 1 0 0;");

        Label listTitle = new Label("MY LIBRARY");
        listTitle.setTextFill(Color.web("#888888"));
        listTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        playlistView = new ListView<>();
        // 去除默认背景，自定义样式
        playlistView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        playlistView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setTextFill(Color.WHITE);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 0 5 0;");
                }
            }
        });
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        // 双击切歌
        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) playSong(idx);
            }
        });

        Button btnAdd = createStyledButton("➕ Import Music", false);
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> addMusic(primaryStage));

        leftPanel.getChildren().addAll(listTitle, playlistView, btnAdd);
        root.setLeft(leftPanel);

        // --- 3. 中间：封面与控制台 ---
        VBox centerPanel = new VBox(25);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(30));

        // 3.1 封面区域 (目前是默认占位符)
        coverPane = createDefaultCover();

        // 3.2 信息区域
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER);
        titleLabel = new Label("EchoPlayer V3");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));

        artistLabel = new Label("Ready to play music");
        artistLabel.setTextFill(Color.web("#AAAAAA"));
        artistLabel.setFont(Font.font("Microsoft YaHei", 14));

        infoBox.getChildren().addAll(titleLabel, artistLabel);

        // 3.3 进度条区域
        VBox progressBox = new VBox(5);
        progressSlider = new Slider();
        progressSlider.setStyle("-fx-control-inner-background: #555555;");
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setTextFill(Color.GRAY);
        timeLabel.setFont(Font.font(10));
        // 让时间显示在右边
        HBox timeContainer = new HBox(timeLabel);
        timeContainer.setAlignment(Pos.CENTER_RIGHT);

        progressBox.getChildren().addAll(progressSlider, timeContainer);

        // 3.4 控制按钮区域
        HBox controls = new HBox(30);
        controls.setAlignment(Pos.CENTER);

        Button btnPrev = createIconButton("⏮");
        btnPlay = createPlayButton(); // 特殊的圆形按钮
        Button btnNext = createIconButton("⏭");

        // 音量小组件
        HBox volBox = new HBox(10);
        volBox.setAlignment(Pos.CENTER);
        Label volIcon = new Label("🔊");
        volIcon.setTextFill(Color.GRAY);
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(80);
        volBox.getChildren().addAll(volIcon, volumeSlider);

        // 组合控制栏
        HBox bottomBar = new HBox(40); // 按钮组和音量组的间距
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.getChildren().addAll(controls, volBox);

        controls.getChildren().addAll(btnPrev, btnPlay, btnNext);
        centerPanel.getChildren().addAll(coverPane, infoBox, progressBox, bottomBar);
        root.setCenter(centerPanel);

        // --- 4. 逻辑绑定 (复用之前的逻辑) ---
        btnPlay.setOnAction(e -> togglePlay());
        btnPrev.setOnAction(e -> playPrev());
        btnNext.setOnAction(e -> playNextSong());

        setupSliderListeners();

        // --- 5. 启动 ---
        Scene scene = new Scene(root, 900, 600); // 窗口更大了
        primaryStage.setTitle("EchoPlayer V3");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadProjectMusic();
        loadSavedPlaylist();
    }

    // --- 界面美化辅助方法 ---

    // 创建默认的唱片封面 (带阴影的深色方块 + 音符)
    private StackPane createDefaultCover() {
        StackPane pane = new StackPane();
        pane.setMaxSize(250, 250);
        pane.setMinSize(250, 250);

        // 背景方块
        Rectangle bg = new Rectangle(250, 250);
        bg.setArcWidth(20);
        bg.setArcHeight(20);
        bg.setFill(Color.web("#222222"));
        // 阴影效果
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(20);
        bg.setEffect(shadow);

        // 音符图标
        Text icon = new Text("🎵");
        icon.setFill(Color.web("#444444"));
        icon.setFont(Font.font(80));

        pane.getChildren().addAll(bg, icon);
        return pane;
    }

    // 创建普通的圆形图标按钮 (上一首/下一首)
    private Button createIconButton(String icon) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 20px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 20px; -fx-background-radius: 50;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 20px;"));
        return btn;
    }

    // 创建大的圆形播放按钮
    private Button createPlayButton() {
        Button btn = new Button("▶"); // 初始状态
        btn.setShape(new Circle(25));
        btn.setMinSize(50, 50);
        btn.setMaxSize(50, 50);

        String styleNormal = "-fx-background-color: white; -fx-text-fill: #1c1c1c; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 50;";
        String styleHover = "-fx-background-color: #dddddd; -fx-text-fill: #1c1c1c; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 50;";

        btn.setStyle(styleNormal);
        btn.setOnMouseEntered(e -> btn.setStyle(styleHover));
        btn.setOnMouseExited(e -> btn.setStyle(styleNormal));
        return btn;
    }

    // 创建普通文字按钮
    private Button createStyledButton(String text, boolean highlight) {
        Button btn = new Button(text);
        String baseStyle = "-fx-text-fill: #dddddd; -fx-font-size: 12px; -fx-background-radius: 5px; -fx-border-color: #555555; -fx-border-radius: 5px;";
        String bg = highlight ? "-fx-background-color: #444444;" : "-fx-background-color: transparent;";

        btn.setStyle(baseStyle + bg);
        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + "-fx-background-color: #555555;"));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle + bg));
        return btn;
    }

    // --- 业务逻辑 (精简版) ---

    private void togglePlay() {
        if (mediaPlayer == null && !playList.isEmpty()) {
            int idx = playlistView.getSelectionModel().getSelectedIndex();
            playSong(idx >= 0 ? idx : 0);
        } else if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                btnPlay.setText("▶"); // 恢复播放图标
            } else {
                mediaPlayer.play();
                btnPlay.setText("⏸"); // 暂停图标
            }
        }
    }

    private void playPrev() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex - 1;
        if (newIndex < 0) newIndex = playList.size() - 1;
        playSong(newIndex);
    }

    private void playSong(int index) {
        if (index < 0 || index >= playList.size()) return;
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }

        currentIndex = index;
        File file = playList.get(index);

        playlistView.getSelectionModel().select(index);
        // 更新大标题
        titleLabel.setText(file.getName().replace(".mp3", "").replace(".wav", ""));
        artistLabel.setText("Playing...");
        btnPlay.setText("⏸");

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.play();

            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue((newT.toMillis() / media.getDuration().toMillis()) * 100);
                }
                timeLabel.setText(formatTime(newT) + " / " + formatTime(media.getDuration()));
            });

            mediaPlayer.setOnEndOfMedia(this::playNextSong);

        } catch (Exception e) {
            artistLabel.setText("Error: " + e.getMessage());
        }
    }

    // ... (以下是之前的 playNextSong, addMusic, data loading, formatTime 等逻辑，保持不变) ...
    // 为了节省篇幅，这里复用了之前的逻辑，你只需要把下面的代码补全即可

    private void playNextSong() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex + 1;
        if (newIndex >= playList.size()) newIndex = 0;
        playSong(newIndex);
    }

    private void addMusic(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) { for (File f : files) addToPlaylistSafe(f); }
    }

    private void loadProjectMusic() {
        File folder = new File("music");
        if (!folder.exists()) { folder.mkdir(); return; }
        File[] files = folder.listFiles((d, n) -> n.toLowerCase().endsWith(".mp3") || n.toLowerCase().endsWith(".wav"));
        if (files != null) { for (File f : files) addToPlaylistSafe(f); }
    }

    private void loadSavedPlaylist() {
        try {
            File f = new File("playlist.txt");
            if (f.exists()) {
                List<String> lines = Files.readAllLines(Paths.get(f.toURI()));
                for (String path : lines) {
                    File file = new File(path);
                    if (file.exists()) addToPlaylistSafe(file);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addToPlaylistSafe(File file) {
        if (playList.stream().noneMatch(f -> f.getName().equals(file.getName()))) {
            playList.add(file);
            playlistView.getItems().add(file.getName());
        }
    }

    @Override public void stop() throws Exception {
        super.stop();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("playlist.txt"))) {
            for (File f : playList) { writer.write(f.getAbsolutePath()); writer.newLine(); }
        }
    }

    private void setupSliderListeners() {
        volumeSlider.valueProperty().addListener((o, ov, nv) -> { if (mediaPlayer != null) mediaPlayer.setVolume(nv.doubleValue()); });
        progressSlider.valueProperty().addListener((o, ov, nv) -> { if (progressSlider.isValueChanging() && mediaPlayer != null) mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(nv.doubleValue() / 100.0)); });
        progressSlider.setOnMouseClicked(e -> { if (mediaPlayer != null) mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(e.getX() / progressSlider.getWidth())); });
    }

    private String formatTime(Duration d) {
        if (d == null) return "00:00";
        int s = (int) d.toSeconds();
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public static void main(String[] args) { launch(args); }
}
