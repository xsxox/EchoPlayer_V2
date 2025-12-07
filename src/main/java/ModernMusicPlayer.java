import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.*;
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
import java.util.Base64;
import java.util.List;

public class ModernMusicPlayer extends Application {

    private MediaPlayer mediaPlayer;
    private List<File> playList = new ArrayList<>();
    private int currentIndex = -1;

    // --- UI 变量 ---
    private BorderPane root;
    private VBox leftPanel;
    private ListView<String> playlistView;
    private Label listTitle;
    private Label titleLabel;
    private Label artistLabel;
    private Label timeLabel;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Button btnPlay;
    private Button btnPrev;
    private Button btnNext;
    private Button btnAdd;
    private Label volIcon;
    private ComboBox<String> themeSelector;

    // --- 样式状态变量 ---
    // 用来暂存播放按钮的基础样式（颜色、阴影），以便动态调整字体大小时不丢失背景
    private String currentPlayBtnStyleBase = "";

    // --- 黑胶组件 ---
    private StackPane vinylRecord;
    private Circle disc;
    private Circle labelCenter;
    private Text vinylText;
    private RotateTransition rotateAnimation;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();

        // --- 左侧 ---
        leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(25));
        leftPanel.setPrefWidth(280);

        listTitle = new Label("LIBRARY");
        listTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 13));

        playlistView = new ListView<>();
        playlistView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(playlistView, Priority.ALWAYS);

        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) playSong(idx);
            }
        });

        btnAdd = new Button("➕ IMPORT TRACKS");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setPrefHeight(45);
        btnAdd.setOnAction(e -> addMusic(primaryStage));

        // 主题选择器
        themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("🌑 Classic Dark", "⚪️ Apple Clean", "👾 Cyberpunk", "💧 Dynamic Blue");
        themeSelector.setValue("🌑 Classic Dark");
        themeSelector.setOnAction(e -> applyTheme(themeSelector.getValue()));
        themeSelector.setMaxWidth(Double.MAX_VALUE);
        themeSelector.setPrefHeight(35);

        VBox bottomLeft = new VBox(15, btnAdd, themeSelector);
        leftPanel.getChildren().addAll(listTitle, playlistView, bottomLeft);
        root.setLeft(leftPanel);

        // --- 中间 ---
        VBox centerPanel = new VBox(30);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPadding(new Insets(40));

        // 1. 黑胶
        createVinylRecord();

        // 2. 信息
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER);
        titleLabel = new Label("EchoPlayer");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 28));

        artistLabel = new Label("System Ready");
        artistLabel.setFont(Font.font("Microsoft YaHei", FontWeight.NORMAL, 16));

        infoBox.getChildren().addAll(titleLabel, artistLabel);

        // 3. 进度
        VBox progressBox = new VBox(8);
        progressSlider = new Slider();
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        HBox timeContainer = new HBox(timeLabel);
        timeContainer.setAlignment(Pos.CENTER_RIGHT);
        progressBox.getChildren().addAll(progressSlider, timeContainer);

        // 4. 按钮
        HBox controls = new HBox(35);
        controls.setAlignment(Pos.CENTER);
        btnPrev = createIconButton("⏮");
        btnPlay = createPlayButton(); // 注意：这里只创建对象，样式在 applyTheme 中初始化
        btnNext = createIconButton("⏭");

        HBox volBox = new HBox(10);
        volBox.setAlignment(Pos.CENTER);
        volIcon = new Label("🔊");
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volBox.getChildren().addAll(volIcon, volumeSlider);

        HBox bottomBar = new HBox(50);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.getChildren().addAll(controls, volBox);

        controls.getChildren().addAll(btnPrev, btnPlay, btnNext);
        centerPanel.getChildren().addAll(vinylRecord, infoBox, progressBox, bottomBar);
        root.setCenter(centerPanel);

        // --- 绑定 ---
        btnPlay.setOnAction(e -> togglePlay());
        btnPrev.setOnAction(e -> playPrev());
        btnNext.setOnAction(e -> playNextSong());
        setupSliderListeners();

        // --- 启动 ---
        // 先应用一次主题，初始化样式
        applyTheme("🌑 Classic Dark");

        Scene scene = new Scene(root, 1050, 720);

        // ✨✨✨ 注入隐藏滚动条的 CSS ✨✨✨
        hideScrollBars(scene);

        primaryStage.setTitle("EchoPlayer V10 - Final Perfect UI");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadProjectMusic();
        loadSavedPlaylist();
    }

    // ==========================================
    //   🛠 CSS 注入工具 (隐藏滚动条专用)
    // ==========================================
    private void hideScrollBars(Scene scene) {
        String css =
                ".list-view .scroll-bar:horizontal {" +
                        "    -fx-pref-height: 0;" +
                        "    -fx-opacity: 0;" +
                        "}" +
                        ".list-view .scroll-bar:vertical {" +
                        "    -fx-pref-width: 0;" +
                        "    -fx-opacity: 0;" +
                        "}" +
                        ".list-view .corner {" +
                        "    -fx-background-color: transparent;" +
                        "}";

        try {
            String base64Css = Base64.getEncoder().encodeToString(css.getBytes("UTF-8"));
            scene.getStylesheets().add("data:text/css;base64," + base64Css);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    //   🎨 主题核心逻辑
    // ==========================================
    private void applyTheme(String themeName) {
        String bgRoot, bgLeft, textMain, textSub, accentColor, sliderTrack;
        String comboBg, comboText, comboBorder, comboHover;
        String playBtnStyle;

        boolean isLightMode = false;
        boolean isCyberpunk = false;
        boolean isDynamicBlue = false;

        // --- 1. 定义配色方案 ---
        switch (themeName) {
            case "⚪️ Apple Clean":
                bgRoot = "linear-gradient(to bottom right, #FFFFFF, #F2F2F7)";
                bgLeft = "rgba(245, 245, 247, 0.8)";
                textMain = "#1C1C1E";
                textSub = "#8E8E93";
                accentColor = "#FA2D48";
                sliderTrack = "#E5E5EA";

                comboBg = "#FFFFFF";
                comboText = "#1C1C1E";
                comboBorder = "#D1D1D6";
                comboHover = "#F2F2F7";

                playBtnStyle = "-fx-background-color: linear-gradient(to bottom right, #FF2D55, #FF5E3A); -fx-text-fill: white;";
                isLightMode = true;
                break;

            case "🌑 Classic Dark":
                bgRoot = "linear-gradient(to bottom, #121212, #181818)";
                bgLeft = "#000000";
                textMain = "#FFFFFF";
                textSub = "#B3B3B3";
                accentColor = "#1DB954";
                sliderTrack = "#404040";

                comboBg = "#282828";
                comboText = "#FFFFFF";
                comboBorder = "#404040";
                comboHover = "#3E3E3E";

                playBtnStyle = "-fx-background-color: #FFFFFF; -fx-text-fill: #000000;";
                break;

            case "👾 Cyberpunk":
                bgRoot = "linear-gradient(to bottom right, #0b0b19, #1a1a3d)";
                bgLeft = "#13132b";
                textMain = "#00f3ff";
                textSub = "#ff0099";
                accentColor = "#00f3ff";
                sliderTrack = "#2a2a40";
                comboBg = "#2a2a40";
                comboText = "#00f3ff";
                comboBorder = "#ff0099";
                comboHover = "#3d3d5c";
                playBtnStyle = "-fx-background-color: #00f3ff; -fx-text-fill: #000000;";
                isCyberpunk = true;
                break;

            case "💧 Dynamic Blue":
            default:
                bgRoot = "linear-gradient(to bottom, #0f172a, #1e293b)";
                bgLeft = "#0f172a";
                textMain = "#e0f2fe";
                textSub = "#94a3b8";
                accentColor = "#38bdf8";
                sliderTrack = "#334155";
                comboBg = "#1e293b";
                comboText = "#ffffff";
                comboBorder = "#38bdf8";
                comboHover = "#334155";
                playBtnStyle = "-fx-background-color: #38bdf8; -fx-text-fill: #000000;";
                isDynamicBlue = true;
                break;
        }

        // --- 2. 应用基础背景与文字 ---
        root.setStyle("-fx-background-color: " + bgRoot + ";");
        leftPanel.setStyle("-fx-background-color: " + bgLeft + "; -fx-border-color: transparent;");

        listTitle.setTextFill(Color.web(accentColor));
        titleLabel.setTextFill(Color.web(textMain));
        artistLabel.setTextFill(Color.web(textSub));
        timeLabel.setTextFill(Color.web(textSub));
        volIcon.setTextFill(Color.web(textSub));

        if (isCyberpunk) {
            titleLabel.setEffect(new Glow(0.8));
            artistLabel.setEffect(new DropShadow(10, Color.web("#ff0099")));
        } else {
            titleLabel.setEffect(null);
            artistLabel.setEffect(null);
        }

        // --- 3. 优化滑块 ---
        String sliderStyle = String.format(
                "-fx-control-inner-background: %s; -fx-accent: %s; -fx-background-color: transparent;",
                sliderTrack, accentColor
        );
        progressSlider.setStyle(sliderStyle);
        volumeSlider.setStyle(sliderStyle);

        // --- 4. 优化列表 (字体加大，无滚动条) ---
        final String finalMainText = textMain;
        final String hoverColor = isLightMode ? "rgba(0,0,0,0.05)" : "rgba(255,255,255,0.08)";
        final String selectionColor = isLightMode ? "#E5E5EA" : (isCyberpunk ? "rgba(0, 243, 255, 0.2)" : "#333333");

        playlistView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setTextFill(Color.web(finalMainText));

                    // ✨ 字体改为 16px，Padding 加大
                    String baseStyle = "-fx-font-size: 16px; -fx-padding: 12 15 12 15; -fx-background-radius: 8;";

                    if (isSelected()) {
                        setStyle(baseStyle + "-fx-background-color: " + selectionColor + "; -fx-font-weight: bold;");
                    } else {
                        setStyle(baseStyle + "-fx-background-color: transparent;");
                    }

                    setOnMouseEntered(e -> {
                        if (!isSelected()) setStyle(baseStyle + "-fx-background-color: " + hoverColor + ";");
                    });
                    setOnMouseExited(e -> {
                        if (!isSelected()) setStyle(baseStyle + "-fx-background-color: transparent;");
                    });
                }
            }
        });
        playlistView.refresh();

        // --- 5. 优化下拉框 ---
        themeSelector.setStyle(
                "-fx-background-color: " + comboBg + "; " +
                        "-fx-font-size: 13px; " +
                        "-fx-text-fill: " + comboText + ";" +
                        "-fx-border-color: " + comboBorder + "; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6;"
        );

        final String fComboBg = comboBg;
        final String fComboText = comboText;
        final String fComboHover = comboHover;

        themeSelector.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setText(item);
                    setTextFill(Color.web(fComboText));
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        themeSelector.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: " + fComboBg + ";");
                } else {
                    setText(item);
                    setTextFill(Color.web(fComboText));
                    setStyle("-fx-background-color: " + fComboBg + "; -fx-padding: 8 10 8 10;");
                    setOnMouseEntered(e -> setStyle("-fx-background-color: " + fComboHover + "; -fx-padding: 8 10 8 10;"));
                    setOnMouseExited(e -> setStyle("-fx-background-color: " + fComboBg + "; -fx-padding: 8 10 8 10;"));
                }
            }
        });

        // --- 6. 优化按钮 (带状态动态调整) ---

        // 1. 保存当前主题的背景样式基础
        currentPlayBtnStyleBase = playBtnStyle +
                "-fx-background-radius: 100; " +
                "-fx-min-width: 65px; " +
                "-fx-min-height: 65px; " +
                "-fx-cursor: hand;";

        // 2. 初始应用“暂停状态”的图标（即三角形），根据当前播放器状态决定
        boolean isPlaying = (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING);
        updatePlayButtonIconStyle(isPlaying);

        // 3. 阴影效果
        if(themeName.equals("⚪️ Apple Clean")) {
            btnPlay.setEffect(new DropShadow(15, Color.rgb(255, 45, 85, 0.4)));
        } else if (themeName.equals("🌑 Classic Dark")) {
            btnPlay.setEffect(new DropShadow(10, Color.rgb(255, 255, 255, 0.2)));
        } else {
            btnPlay.setEffect(isCyberpunk ? new DropShadow(15, Color.web("#00f3ff")) : null);
        }

        addHoverAnimation(btnPlay);

        updateButtonStyle(btnAdd, textSub, accentColor, isLightMode);
        updateButtonStyle(btnPrev, textMain, accentColor, isLightMode);
        updateButtonStyle(btnNext, textMain, accentColor, isLightMode);

        updateVinylStyle(themeName);
    }

    // ==========================================
    //   🔧 专门用来修播放/暂停图标大小不对齐的方法
    // ==========================================
    private void updatePlayButtonIconStyle(boolean isPlaying) {
        if (isPlaying) {
            // ⏸ 暂停图标状态 (双竖线)
            // 修正：字体改小(28px)，Padding归零，保证完美居中
            btnPlay.setStyle(currentPlayBtnStyleBase +
                    "-fx-font-size: 30px; " +
                    "-fx-padding: 0;"
            );
            btnPlay.setText("⏸");
        } else {
            // ▶ 播放图标状态 (三角形)
            // 修正：字体改大(36px)，左边距加4px，解决视觉偏左问题
            btnPlay.setStyle(currentPlayBtnStyleBase +
                    "-fx-font-size: 36px; " +
                    "-fx-padding: 0 0 0 4;"
            );
            btnPlay.setText("▶");
        }
    }

    private void updateButtonStyle(Button btn, String normalColor, String hoverColor, boolean isLight) {
        String borderColor = (btn == btnAdd) ? (isLight ? "#C7C7CC" : "#3A3A3C") : "transparent";
        String hoverBorder = (btn == btnAdd) ? hoverColor : "transparent";
        String size = (btn == btnAdd) ? "12" : "24";
        String fontWeight = (btn == btnAdd) ? "bold" : "normal";

        String base =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + normalColor + "; " +
                        "-fx-font-size: " + size + "px; " +
                        "-fx-font-weight: " + fontWeight + "; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;";

        String hover =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: " + hoverColor + "; " +
                        "-fx-font-size: " + size + "px; " +
                        "-fx-font-weight: " + fontWeight + "; " +
                        "-fx-border-color: " + hoverBorder + "; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;";

        btn.setStyle(base);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(hover);
            btn.setScaleX(1.1);
            btn.setScaleY(1.1);
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(base);
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
    }

    private void addHoverAnimation(Button btn) {
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.1);
            btn.setScaleY(1.1);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
    }

    private void updateVinylStyle(String theme) {
        disc.setFill(Color.BLACK);
        vinylText.setFill(Color.WHITE);

        if (theme.equals("⚪️ Apple Clean")) {
            disc.setFill(Color.web("#2C2C2E"));
            disc.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.15)));
            vinylText.setFill(Color.web("#E5E5EA"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#FF5E3A")),
                    new Stop(1, Color.web("#FF2D55"))));

        } else if (theme.equals("🌑 Classic Dark")) {
            disc.setFill(Color.web("#121212"));
            disc.setEffect(new DropShadow(15, Color.rgb(255,255,255,0.05)));
            vinylText.setFill(Color.web("#AAAAAA"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#1DB954")),
                    new Stop(1, Color.web("#191414"))));

        } else if (theme.equals("👾 Cyberpunk")) {
            disc.setFill(Color.BLACK);
            disc.setEffect(new DropShadow(20, Color.web("#00f3ff")));
            vinylText.setFill(Color.web("#00f3ff"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#00f3ff")),
                    new Stop(1, Color.web("#ff0099"))));

        } else if (theme.equals("💧 Dynamic Blue")) {
            disc.setFill(Color.web("#020617"));
            disc.setEffect(new DropShadow(20, Color.web("#38bdf8")));
            vinylText.setFill(Color.web("#e0f2fe"));
            labelCenter.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#7dd3fc")),
                    new Stop(1, Color.web("#0ea5e9"))));
        }
    }

    // ==========================================
    //   核心黑胶创建
    // ==========================================
    private void createVinylRecord() {
        vinylRecord = new StackPane();
        vinylRecord.setMaxSize(280, 280);
        vinylRecord.setMinSize(280, 280);

        disc = new Circle(140);

        Circle groove1 = new Circle(125);
        groove1.setFill(Color.TRANSPARENT);
        groove1.setStroke(Color.rgb(255,255,255,0.1));
        groove1.setStrokeWidth(2);

        Circle groove2 = new Circle(105);
        groove2.setFill(Color.TRANSPARENT);
        groove2.setStroke(Color.rgb(255,255,255,0.1));
        groove2.setStrokeWidth(2);

        Rectangle shine = new Rectangle(20, 260);
        shine.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.5, Color.rgb(255, 255, 255, 0.1)),
                new Stop(1, Color.TRANSPARENT)));
        shine.setRotate(45);

        labelCenter = new Circle(50);
        vinylText = new Text("ECHO");
        vinylText.setFont(Font.font("Verdana", FontWeight.BOLD, 16));

        Circle hole = new Circle(6, Color.BLACK);

        vinylRecord.getChildren().addAll(disc, groove1, groove2, shine, labelCenter, vinylText, hole);

        rotateAnimation = new RotateTransition(Duration.seconds(6), vinylRecord);
        rotateAnimation.setByAngle(360);
        rotateAnimation.setCycleCount(RotateTransition.INDEFINITE);
        rotateAnimation.setInterpolator(Interpolator.LINEAR);
    }

    // --- 播放逻辑 ---
    private void togglePlay() {
        if (mediaPlayer == null && !playList.isEmpty()) {
            int idx = playlistView.getSelectionModel().getSelectedIndex();
            playSong(idx >= 0 ? idx : 0);
        } else if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                rotateAnimation.pause();

                // 切换到“未播放”状态 (显示三角形)
                updatePlayButtonIconStyle(false);

            } else {
                mediaPlayer.play();
                rotateAnimation.play();

                // 切换到“播放中”状态 (显示双竖线)
                updatePlayButtonIconStyle(true);
            }
        }
    }

    private void playSong(int index) {
        if (index < 0 || index >= playList.size()) return;
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }

        currentIndex = index;
        File file = playList.get(index);

        playlistView.getSelectionModel().select(index);
        titleLabel.setText(file.getName().replace(".mp3", ""));
        artistLabel.setText("Now Playing");

        // 开始播放 -> 显示暂停图标
        updatePlayButtonIconStyle(true);

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.play();

            vinylRecord.setRotate(0);
            rotateAnimation.playFromStart();

            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue((newT.toMillis() / media.getDuration().toMillis()) * 100);
                }
                timeLabel.setText(formatTime(newT) + " / " + formatTime(media.getDuration()));
            });

            mediaPlayer.setOnEndOfMedia(this::playNextSong);

        } catch (Exception e) {
            artistLabel.setText("Load Error");
        }
    }

    private void playNextSong() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex + 1;
        if (newIndex >= playList.size()) newIndex = 0;
        playSong(newIndex);
    }

    private void playPrev() {
        if (playList.isEmpty()) return;
        int newIndex = currentIndex - 1;
        if (newIndex < 0) newIndex = playList.size() - 1;
        playSong(newIndex);
    }

    // --- 数据加载 ---
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

    private Button createIconButton(String icon) {
        Button btn = new Button(icon);
        return btn;
    }

    private Button createPlayButton() {
        Button btn = new Button("▶");
        return btn;
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
