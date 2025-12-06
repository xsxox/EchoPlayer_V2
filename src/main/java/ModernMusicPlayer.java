import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModernMusicPlayer extends Application {

    private MediaPlayer mediaPlayer;

    // --- 新增：播放列表相关变量 ---
    private List<File> playList = new ArrayList<>(); // 存文件
    private int currentIndex = -1;                   // 当前播到第几首
    private ListView<String> playlistView;           // 界面上的列表控件

    // 界面组件
    private Label statusLabel;
    private Label timeLabel;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Button btnPlay; // 把播放按钮提出来，方便改变图标

    @Override
    public void start(Stage primaryStage) {
        // --- 1. 整体布局：使用 BorderPane (分上下左右中) ---
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

        // --- 2. 左侧：播放列表区域 ---
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(200); // 宽度固定 200
        leftPanel.setStyle("-fx-background-color: #333333;");

        Label listTitle = new Label("📜 播放列表");
        listTitle.setTextFill(Color.WHITE);
        listTitle.setFont(new Font(16));

        // 列表控件
        playlistView = new ListView<>();
        playlistView.setStyle("-fx-background-color: #333333; -fx-control-inner-background: #333333; -fx-text-fill: white;");
        VBox.setVgrow(playlistView, Priority.ALWAYS); // 让列表占满剩余高度

        // 双击列表切歌
        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // 双击
                int selectedIndex = playlistView.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    playSong(selectedIndex);
                }
            }
        });

        // 添加文件按钮
        Button btnAdd = createStyledButton("➕ 添加音乐");
        btnAdd.setMaxWidth(Double.MAX_VALUE); // 按钮撑满宽度
        btnAdd.setOnAction(e -> addMusic(primaryStage));

        leftPanel.getChildren().addAll(listTitle, btnAdd, playlistView);
        root.setLeft(leftPanel); // 放到左边

        // --- 3. 中部/底部：控制区域 ---
        VBox centerPanel = new VBox(20);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(20));

        // 歌名显示
        statusLabel = new Label("ECHO PLAYER");
        statusLabel.setFont(new Font("Microsoft YaHei", 24));
        statusLabel.setTextFill(Color.WHITE);

        // 时间
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setTextFill(Color.CYAN);

        // 进度条
        progressSlider = new Slider();
        progressSlider.setDisable(true);

        // 按钮组
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);

        Button btnPrev = createStyledButton("⏮ 上一首");
        btnPlay = createStyledButton("▶ 播放"); // 注意这里还没写逻辑
        Button btnNext = createStyledButton("⏭ 下一首");

        // 音量
        Label volLabel = new Label("🔊");
        volLabel.setTextFill(Color.WHITE);
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setMaxWidth(100);

        controls.getChildren().addAll(btnPrev, btnPlay, btnNext, volLabel, volumeSlider);

        centerPanel.getChildren().addAll(statusLabel, timeLabel, progressSlider, controls);
        root.setCenter(centerPanel); // 放到中间

        // --- 4. 按钮逻辑 ---

        // 播放/暂停
        btnPlay.setOnAction(e -> {
            if (mediaPlayer == null && !playList.isEmpty()) {
                playSong(0); // 如果没在播，就从第一首开始
            } else if (mediaPlayer != null) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    btnPlay.setText("▶ 播放");
                } else {
                    mediaPlayer.play();
                    btnPlay.setText("⏸ 暂停");
                }
            }
        });

        // 上一首
        btnPrev.setOnAction(e -> {
            if (playList.isEmpty()) return;
            int newIndex = currentIndex - 1;
            if (newIndex < 0) newIndex = playList.size() - 1; // 循环到最后一首
            playSong(newIndex);
        });

        // 下一首
        btnNext.setOnAction(e -> {
            playNextSong();
        });

        // 音量和进度条逻辑保持不变
        setupSliderListeners();

        // --- 5. 启动 ---
        Scene scene = new Scene(root, 700, 400); // 窗口变大一点
        primaryStage.setTitle("EchoPlayer V2 - 播放列表版");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- 核心方法：添加音乐 ---
    private void addMusic(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("添加音乐");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav"));

        // 允许选择多个文件
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null) {
            playList.addAll(files); // 加到数据列表
            for (File f : files) {
                playlistView.getItems().add(f.getName()); // 加到界面列表
            }
        }
    }

    // --- 核心方法：播放指定位置的歌 ---
    private void playSong(int index) {
        if (index < 0 || index >= playList.size()) return;

        // 停止之前的
        if (mediaPlayer != null) mediaPlayer.dispose();

        currentIndex = index;
        File file = playList.get(index);

        // 界面联动：选中列表中的那一行
        playlistView.getSelectionModel().select(index);
        statusLabel.setText(file.getName());
        btnPlay.setText("⏸ 暂停");
        progressSlider.setDisable(false);

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.play();

            // 监听进度
            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue((newT.toMillis() / media.getDuration().toMillis()) * 100);
                }
                updateTimeLabel(newT, media.getDuration());
            });

            // 监听：这首歌播完自动下一首
            mediaPlayer.setOnEndOfMedia(() -> {
                playNextSong();
            });

        } catch (Exception e) {
            statusLabel.setText("播放出错: " + e.getMessage());
        }
    }

    // --- 核心方法：播放下一首 ---
    private void playNextSong() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex + 1;
        if (newIndex >= playList.size()) newIndex = 0; // 循环回到第一首
        playSong(newIndex);
    }

    // 辅助：设置进度条拖拽监听 (逻辑和之前一样)
    private void setupSliderListeners() {
        volumeSlider.valueProperty().addListener((o, oldV, newV) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newV.doubleValue());
        });

        progressSlider.valueProperty().addListener((o, oldV, newV) -> {
            if (progressSlider.isValueChanging() && mediaPlayer != null) {
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(newV.doubleValue() / 100.0));
            }
        });
    }

    private void updateTimeLabel(Duration current, Duration total) {
        timeLabel.setText(formatTime(current) + " / " + formatTime(total));
    }

    private String formatTime(Duration d) {
        int seconds = (int) d.toSeconds();
        int minutes = seconds / 60;
        return String.format("%02d:%02d", minutes, seconds % 60);
    }

    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        String style = "-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 5px;";
        btn.setStyle(style);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #5c6bc0; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 5px;"));
        btn.setOnMouseExited(e -> btn.setStyle(style));
        return btn;
    }

    // 入口
    public static void main(String[] args) {
        launch(args);
    }
}
