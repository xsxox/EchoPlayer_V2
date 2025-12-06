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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ModernMusicPlayer extends Application {

    private MediaPlayer mediaPlayer;

    // --- 核心数据 ---
    private List<File> playList = new ArrayList<>(); // 内存中的歌曲文件列表
    private int currentIndex = -1;                   // 当前正在播放的索引

    // --- 界面控件 ---
    private ListView<String> playlistView;
    private Label statusLabel;
    private Label timeLabel;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Button btnPlay;

    @Override
    public void start(Stage primaryStage) {
        // --- 1. 整体布局 ---
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

        // --- 2. 左侧：播放列表区域 ---
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(220);
        leftPanel.setStyle("-fx-background-color: #333333;");

        Label listTitle = new Label("📜 混合歌单");
        listTitle.setTextFill(Color.WHITE);
        listTitle.setFont(new Font("Microsoft YaHei", 16));

        // 列表视图
        playlistView = new ListView<>();
        playlistView.setStyle("-fx-background-color: #333333; -fx-control-inner-background: #333333; -fx-text-fill: white;");
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        // 双击切歌事件
        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = playlistView.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    playSong(selectedIndex);
                }
            }
        });

        // 手动添加按钮
        Button btnAdd = createStyledButton("➕ 添加本地文件");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> addMusic(primaryStage));

        leftPanel.getChildren().addAll(listTitle, btnAdd, playlistView);
        root.setLeft(leftPanel);

        // --- 3. 中部：控制台区域 ---
        VBox centerPanel = new VBox(20);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(20));

        statusLabel = new Label("ECHO PLAYER");
        statusLabel.setFont(new Font("Microsoft YaHei", 20));
        statusLabel.setTextFill(Color.WHITE);

        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setTextFill(Color.CYAN);

        progressSlider = new Slider();
        progressSlider.setDisable(true);

        // 按钮组
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);

        Button btnPrev = createStyledButton("⏮ 上一首");
        btnPlay = createStyledButton("▶ 播放");
        Button btnNext = createStyledButton("⏭ 下一首");

        Label volLabel = new Label("🔊");
        volLabel.setTextFill(Color.WHITE);
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setMaxWidth(100);

        controls.getChildren().addAll(btnPrev, btnPlay, btnNext, volLabel, volumeSlider);

        centerPanel.getChildren().addAll(statusLabel, timeLabel, progressSlider, controls);
        root.setCenter(centerPanel);

        // --- 4. 按钮逻辑 ---
        btnPlay.setOnAction(e -> {
            if (mediaPlayer == null && !playList.isEmpty()) {
                int selectIndex = playlistView.getSelectionModel().getSelectedIndex();
                playSong(selectIndex >= 0 ? selectIndex : 0);
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

        btnPrev.setOnAction(e -> {
            if (playList.isEmpty()) return;
            int newIndex = currentIndex - 1;
            if (newIndex < 0) newIndex = playList.size() - 1;
            playSong(newIndex);
        });

        btnNext.setOnAction(e -> playNextSong());

        setupSliderListeners();

        // --- 5. 启动 ---
        Scene scene = new Scene(root, 750, 450);
        primaryStage.setTitle("EchoPlayer V2 - 完美混合版");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 🔥 关键逻辑：先加载内置，再加载记忆
        loadProjectMusic();
        loadSavedPlaylist();
    }

    // --- 退出时保存 ---
    @Override
    public void stop() throws Exception {
        super.stop();
        savePlaylist();
    }

    // ---------------------------------------------------------
    //   数据加载逻辑 (混合双打)
    // ---------------------------------------------------------

    // 1. 加载项目内置 music 文件夹
    private void loadProjectMusic() {
        File musicFolder = new File("music");
        if (!musicFolder.exists()) {
            musicFolder.mkdir();
            return;
        }
        File[] files = musicFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".m4a")
        );
        if (files != null) {
            for (File file : files) {
                addToPlaylistSafe(file); // 使用安全添加方法
            }
        }
    }

    // 2. 加载 playlist.txt 记忆文件
    private void loadSavedPlaylist() {
        File dataFile = new File("playlist.txt");
        if (!dataFile.exists()) return;

        try {
            List<String> paths = Files.readAllLines(Paths.get(dataFile.toURI()));
            for (String path : paths) {
                File file = new File(path);
                // 必须文件存在，且列表里还没有它
                if (file.exists()) {
                    addToPlaylistSafe(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 3. 保存当前列表到文件
    private void savePlaylist() {
        try {
            File dataFile = new File("playlist.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile));
            for (File file : playList) {
                writer.write(file.getAbsolutePath());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 辅助：安全添加（防止重复）
    private void addToPlaylistSafe(File file) {
        // 简单去重：检查文件名是否已存在
        boolean exists = playList.stream().anyMatch(f -> f.getName().equals(file.getName()));
        if (!exists) {
            playList.add(file);
            playlistView.getItems().add(file.getName());
        }
    }

    // ---------------------------------------------------------
    //   播放器核心逻辑 (保持不变)
    // ---------------------------------------------------------

    private void addMusic(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("添加音乐文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) {
            for (File f : files) {
                addToPlaylistSafe(f);
            }
        }
    }

    private void playSong(int index) {
        if (index < 0 || index >= playList.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        currentIndex = index;
        File file = playList.get(index);

        playlistView.getSelectionModel().select(index);
        statusLabel.setText(file.getName());
        btnPlay.setText("⏸ 暂停");
        progressSlider.setDisable(false);

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.play();

            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue((newT.toMillis() / media.getDuration().toMillis()) * 100);
                }
                updateTimeLabel(newT, media.getDuration());
            });

            mediaPlayer.setOnEndOfMedia(this::playNextSong);

        } catch (Exception e) {
            statusLabel.setText("播放失败: " + e.getMessage());
        }
    }

    private void playNextSong() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex + 1;
        if (newIndex >= playList.size()) newIndex = 0;
        playSong(newIndex);
    }

    private void setupSliderListeners() {
        volumeSlider.valueProperty().addListener((o, oldV, newV) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newV.doubleValue());
        });

        progressSlider.valueProperty().addListener((o, oldV, newV) -> {
            if (progressSlider.isValueChanging() && mediaPlayer != null) {
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(newV.doubleValue() / 100.0));
            }
        });

        progressSlider.setOnMouseClicked(event -> {
            if (mediaPlayer != null) {
                double mouseX = event.getX();
                double width = progressSlider.getWidth();
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(mouseX / width));
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

    public static void main(String[] args) {
        launch(args);
    }
}
