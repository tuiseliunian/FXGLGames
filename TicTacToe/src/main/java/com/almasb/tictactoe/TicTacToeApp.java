package com.almasb.tictactoe;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.settings.GameSettings;
import com.almasb.tictactoe.minimax.AIPlayerMinimax;
import com.almasb.tictactoe.minimax.Board;
import com.almasb.tictactoe.minimax.Seed;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * An example of a UI based game.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class TicTacToeApp extends GameApplication {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("TicTacToe");
        settings.setVersion("0.2");
        settings.setWidth(600);
        settings.setHeight(600);
        settings.setIntroEnabled(false);
        settings.setMenuEnabled(false);
        settings.setProfilingEnabled(false);
        settings.setCloseConfirmation(false);
        settings.setApplicationMode(ApplicationMode.DEVELOPER);
    }

    @Override
    protected void initInput() {}

    @Override
    protected void initAssets() {}

    private TileEntity[][] board = new TileEntity[3][3];
    private List<TileCombo> combos = new ArrayList<>();

    private boolean playerStarts = true;

    @Override
    protected void initGame() {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                TileEntity tile = new TileEntity(x * getWidth() / 3, y * getHeight() / 3);
                board[x][y] = tile;

                getGameWorld().addEntity(tile);
            }
        }

        combos.clear();

        // horizontal
        for (int y = 0; y < 3; y++) {
            combos.add(new TileCombo(board[0][y], board[1][y], board[2][y]));
        }

        // vertical
        for (int x = 0; x < 3; x++) {
            combos.add(new TileCombo(board[x][0], board[x][1], board[x][2]));
        }

        // diagonals
        combos.add(new TileCombo(board[0][0], board[1][1], board[2][2]));
        combos.add(new TileCombo(board[2][0], board[1][1], board[0][2]));

        if (playerStarts) {
            playerStarts = false;
        } else {
            aiMove();
            playerStarts = true;
        }
    }

    @Override
    protected void initPhysics() {}

    @Override
    protected void initUI() {
        Line line1 = new Line(getWidth() / 3, 0, getWidth() / 3, 0);
        Line line2 = new Line(getWidth() / 3 * 2, 0, getWidth() / 3 * 2, 0);
        Line line3 = new Line(0, getHeight() / 3, 0, getHeight() / 3);
        Line line4 = new Line(0, getHeight() / 3 * 2, 0, getHeight() / 3 * 2);

        getGameScene().addUINodes(line1, line2, line3, line4);

        // animation
        KeyFrame frame1 = new KeyFrame(Duration.seconds(0.5),
                new KeyValue(line1.endYProperty(), getHeight()));

        KeyFrame frame2 = new KeyFrame(Duration.seconds(1),
                new KeyValue(line2.endYProperty(), getHeight()));

        KeyFrame frame3 = new KeyFrame(Duration.seconds(0.5),
                new KeyValue(line3.endXProperty(), getWidth()));

        KeyFrame frame4 = new KeyFrame(Duration.seconds(1),
                new KeyValue(line4.endXProperty(), getWidth()));

        Timeline timeline = new Timeline(frame1, frame2, frame3, frame4);
        timeline.play();
    }

    @Override
    protected void onUpdate(double tpf) {}

    private boolean checkGameFinished() {
        for (TileCombo combo : combos) {
            if (combo.isComplete()) {
                playWinAnimation(combo);
                return true;
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                TileEntity tile = board[x][y];
                if (tile.getValue() == TileValue.NONE) {
                    // at least 1 tile is empty
                    return false;
                }
            }
        }

        gameOver("DRAW");
        return true;
    }

    private void playWinAnimation(TileCombo combo) {
        Line line = new Line();
        line.setStartX(combo.getTile1().getCenter().getX());
        line.setStartY(combo.getTile1().getCenter().getY());
        line.setEndX(combo.getTile1().getCenter().getX());
        line.setEndY(combo.getTile1().getCenter().getY());
        line.setStroke(Color.YELLOW);
        line.setStrokeWidth(3);

        getGameScene().addUINode(line);

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1),
                new KeyValue(line.endXProperty(), combo.getTile3().getCenter().getX()),
                new KeyValue(line.endYProperty(), combo.getTile3().getCenter().getY())));
        timeline.setOnFinished(e -> gameOver(combo.getWinSymbol()));
        timeline.play();
    }

    private void gameOver(String winner) {
        getDisplay().showConfirmationBox("Winner: " + winner + "\nContinue?", yes -> {
            if (yes)
                startNewGame();
            else
                exit();
        });
    }

    public void onUserMove(TileEntity tile) {
        boolean ok = tile.getControl().mark(TileValue.X);

        if (ok) {
            boolean over = checkGameFinished();

            if (!over) {
                aiMove();
                checkGameFinished();
            }
        }
    }

    private List<Predicate<TileCombo> > aiPredicates = Arrays.asList(
            c -> c.isTwoThirds(TileValue.O),
            c -> c.isTwoThirds(TileValue.X),
            c -> c.isOneThird(TileValue.O),
            c -> c.isOpen(),
            c -> c.getFirstEmpty() != null
    );

    private void aiMove() {
        aiMinimax();
    }

    /**
     * A decent AI but can lose.
     */
    private void aiRuleBased() {
        TileEntity tile = aiPredicates.stream()
                .map(predicate -> {
                    return combos.stream()
                            .filter(predicate)
                            .findAny()
                            .map(TileCombo::getFirstEmpty)
                            .orElse(null);
                })
                .filter(t -> t != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No empty tiles"));

        tile.getControl().mark(TileValue.O);
    }

    private Board copyBoard;
    private AIPlayerMinimax ai;

    /**
     * Unbeatable AI.
     */
    private void aiMinimax() {
        if (copyBoard == null) {
            copyBoard = new Board();
            ai = new AIPlayerMinimax(copyBoard);
            ai.setSeed(Seed.NOUGHT);
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                TileEntity tile = board[x][y];
                if (tile.getValue() == TileValue.X) {
                    copyBoard.setContent(x, y, Seed.CROSS);
                } else if (tile.getValue() == TileValue.O) {
                    copyBoard.setContent(x, y, Seed.NOUGHT);
                } else {
                    copyBoard.setContent(x, y, Seed.EMPTY);
                }
            }
        }

        int[] result = ai.move();
        board[result[1]][result[0]].getControl().mark(TileValue.O);
    }

    public static void main(String[] args) {
        launch(args);
    }
}