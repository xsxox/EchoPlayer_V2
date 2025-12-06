import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

// 继承 Application 是 JavaFX 程序的标准入口
public class ModernMusicPlayer extends Application {

    private MediaPlayer mediaPlayer; // 核心播放器
    private Label statusLabel;       // 显示状态
    private Label timeLabel;         // 显示时间
    private Slider volumeSlider;     // 音量滑块
    private Slider progressSlider;   // 进度条 (新增)
    //
    @Override
    public void start(Stage primaryStage) {
        // --- 1. 布局设计 ---
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        // 深色背景，科技感
        root.setStyle("-fx-background-color: #2b2b2b;");

        // --- 2. 界面组件 ---

        // 标题
        Label titleLabel = new Label("🎵 我的 Java 播放器");
        titleLabel.setFont(new Font("Microsoft YaHei", 24));
        titleLabel.setTextFill(Color.WHITE);

        // 状态文字
        statusLabel = new Label("请选择音乐文件...");
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setFont(new Font("Microsoft YaHei", 14));

        // 时间文字
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setTextFill(Color.CYAN);

        // 进度条
        progressSlider = new Slider();
        progressSlider.setDisable(true); // 没播放时禁止拖动

        // 控制按钮区域
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);

        Button btnOpen = createStyledButton("打开");
        Button btnPlay = createStyledButton("▶ 播放");
        Button btnPause = createStyledButton("⏸ 暂停");
        Button btnStop = createStyledButton("⏹ 停止");

        // 音量区域
        Label volLabel = new Label("🔊");
        volLabel.setTextFill(Color.WHITE);
        volumeSlider = new Slider(0, 1, 0.5); // 0到1，默认0.5
        volumeSlider.setMaxWidth(100);

        controls.getChildren().addAll(btnOpen, btnPlay, btnPause, btnStop, volLabel, volumeSlider);

        // 把所有东西加到主面板
        root.getChildren().addAll(titleLabel, statusLabel, timeLabel, progressSlider, controls);

        // --- 3. 按钮事件逻辑 --

        // 打开
        btnOpen.setOnAction(e -> chooseFile(primaryStage));

        // 播放
        btnPlay.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.play();
                statusLabel.setText("正在播放...");
            }
        });

        // 暂停
        btnPause.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                statusLabel.setText("已暂停");
            }
        });

        // 停止
        btnStop.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                statusLabel.setText("已停止");
            }
        });

        // 音量调节
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue());
        });

        // 进度条拖拽 (用户拖动进度条跳转)
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging() && mediaPlayer != null) {
                // 当用户正在拖拽时，跳转到对应时间
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(newVal.doubleValue() / 100.0));
            }
        });

        // --- 4. 显示窗口 ---
        Scene scene = new Scene(root, 600, 350);
        primaryStage.setTitle("Java MP3 Player");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // 选择文件的方法
    private void chooseFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择音乐文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.m4a", "*.wav")
        );
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            initPlayer(file);
        }
    }

    // 初始化播放器核心
    private void initPlayer(File file) {
        if (mediaPlayer != null) mediaPlayer.dispose(); // 销毁旧的

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.setAutoPlay(true); // 加载完自动播
            statusLabel.setText("正在播放: " + file.getName());
            progressSlider.setDisable(false);

            // 监听播放进度 (让进度条自己走)
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!progressSlider.isValueChanging()) {
                    double total = mediaPlayer.getTotalDuration().toMillis();
                    double current = newTime.toMillis();
                    progressSlider.setValue((current / total) * 100.0);
                }
                updateTimeLabel(newTime, mediaPlayer.getTotalDuration());
            });

            // 播放结束
            mediaPlayer.setOnEndOfMedia(() -> {
                statusLabel.setText("播放结束");
                mediaPlayer.stop();
            });

        } catch (Exception e) {
            statusLabel.setText("无法播放: " + e.getMessage());
        }
    }

    // 格式化时间显示
    private void updateTimeLabel(Duration current, Duration total) {
        String sCurrent = formatTime(current);
        String sTotal = formatTime(total);
        timeLabel.setText(sCurrent + " / " + sTotal);
    }

    private String formatTime(Duration d) {
        int seconds = (int) d.toSeconds();
        int minutes = seconds / 60;
        return String.format("%02d:%02d", minutes, seconds % 60);
    }

    // 创建好看的按钮
    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        String styleNormal = "-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5px;";
        String styleHover = "-fx-background-color: #5c6bc0; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5px;";

        btn.setStyle(styleNormal);
        btn.setOnMouseEntered(e -> btn.setStyle(styleHover));
        btn.setOnMouseExited(e -> btn.setStyle(styleNormal));
        return btn;
    }

    // 留空 main 方法，交给 Launcher 调用
    public static void main(String[] args) {
        launch(args);
    }
}
