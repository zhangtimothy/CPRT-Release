package javachessgui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;

// notes for training mode:
// press space to tell cpu to make a random move
// move list is hidden
// only moves in repertoire are allowed
// APPROACH: each node has a FEN, and list of children moves
public class Repertoire {

    // first 100 primes used in TruncatedFen.hashcode()

    private class RepertoirePosition {
        TruncatedFen fen; // fen that represents this position
        TreeSet<String> nextMoves; // options for next moves contained in this repertoire
        int numDescendants; // for now this is unused. maybe i'll find a use for it later


        public RepertoirePosition(TruncatedFen fen) {
            this.fen = fen;
            nextMoves = new TreeSet<>(); // sorted lexicographically by move
            numDescendants = 0;
        }
    }

    private class RepertoireComment {
        TruncatedFen fen;
        String comment;

        // make using untruncated fen
        public RepertoireComment(String fen) {
            this.fen = new TruncatedFen(fen, false);
            comment = "";
        }

        // make using string array of length 2, first is truncated fen, second is the comment. use when loading COMMENT_DATA
        public RepertoireComment(String[] commentData) {
            fen = new TruncatedFen(commentData[0], true);
            comment = commentData[1];
        }

        public void setComment(String s) {
            comment = s;
        }

        public String getComment() {
            return comment;
        }

        public String toString() {
            return "{" + fen.toString() + "|" + comment + "}\n"; // todo: how should i format my comment file
        }
    }

    private class TruncatedFen {
        String truncatedFen;

        public TruncatedFen(String fen, boolean isTruncated) {
            if (isTruncated)
                truncatedFen = fen;
            else
                truncatedFen = truncateFen(fen);
        }

        public boolean equals(Object o) {
            if (!(o instanceof TruncatedFen))
                return false;
            return this.truncatedFen.equals(((TruncatedFen) o).truncatedFen);
        }

        public int hashCode() {
            return truncatedFen.hashCode();
        }

        public String toString() {
            return truncatedFen;
        }

        // removes last two digits in FEN(halfmove clock and fullmove number) and en passant square IF no pawns can capture
        private String truncateFen(String fen) {
            String[] fenSplit = fen.split(" ");
            String enPassantSquare = fenSplit[3];
            boolean keepEnPassantSquare = false;
            // if length is > 1, it must not be "-", so we have to check if we want to skip it
            if (enPassantSquare.length() > 1) {
                char enPassantFile = enPassantSquare.charAt(0);
                // check if taking en passant is a legal move
                Board b = new Board(false);
                b.set_from_fen(fen);
                if (b.is_san_move_legal(((char) (enPassantFile - 1)) + "x" + enPassantSquare)
                        || b.is_san_move_legal(((char) (enPassantFile + 1)) + "x" + enPassantSquare)) {
                    keepEnPassantSquare = true;
                }
            }
            StringBuilder newFen = new StringBuilder();
            for (int i = 0; i <= 2 + (keepEnPassantSquare ? 1 : 0); i++) {
                if (i > 0) newFen.append(" ");
                newFen.append(fenSplit[i]);
            }

            return newFen.toString();
        }
    }

    private static final String startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private final TruncatedFen startingFenTruncated = new TruncatedFen(startingFen, false);

    // REPERTOIRE INSTANCE FIELDS
    // path to file containing PGNs of games in this repertoire
    String path;
    String name;
    boolean trainingMode;
    HashMap<TruncatedFen, RepertoirePosition> positionMap; // key: FEN, val: node representing that FEN
    HashMap<TruncatedFen, RepertoireComment> commentMap; // key: FEN, val: comments for this position
    TruncatedFen curTruncFen;

    // todo: when i do comments, probably should store them in each node, and each time we import a pgn, append the comments to the comment field of the treenode, to account for multiple games with comments on same move
    // todo: make it so you can click move list and itll make the move. also make it so we can practice and the cpu will pick random moves. maybe then allow user to set frequency of how likely a line will be chosen
    // initialize repertoire, should be used only once. use load() to reset
    public Repertoire() {
        positionMap = new HashMap<>();
        commentMap = new HashMap<>();
        curTruncFen = startingFenTruncated;
        reset();
    }

    private void reset() {
        path = "";
        name = "none";
//        trainingMode = false;
        positionMap.clear();
        positionMap.put(startingFenTruncated, new RepertoirePosition(startingFenTruncated));
        commentMap.clear();
//        curTruncFen = startingFenTruncated;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }


    // starts the repertoire at the given fen
    public void updateFen(String fen) {
//        System.out.println("rep name: " + name);
        TruncatedFen truncFen = new TruncatedFen(fen, false);
        curTruncFen = truncFen;
    }

    public void setTrainingMode(boolean on) {
        trainingMode = on;
    }

    public boolean isTrainingMode() {
        return trainingMode;
    }

    // returns array of strings of next moves and corresponding data, for display in the move book
    public String[] getNextMovesForBook() {
        TruncatedFen curFen = curTruncFen;
        if (trainingMode || !positionMap.containsKey(curFen)) return new String[0];
        RepertoirePosition curNode = positionMap.get(curFen);
        String[] moves = new String[curNode.nextMoves.size()];
        int i = 0;
        for (String move : curNode.nextMoves) {
            moves[i++] = move;
        }
        return moves;
    }

    public boolean containsPosition(String fen) {
        return positionMap.containsKey(new TruncatedFen(fen, false));
    }

    public int getNumAllowedMoves() {
        if (!positionMap.containsKey(curTruncFen)) {
            return 0;
        } else {
            return positionMap.get(curTruncFen).nextMoves.size();
        }
    }

    public String getRandomMove() {
        TruncatedFen curFen = curTruncFen;
        if (!positionMap.containsKey(curFen)) return "";
        RepertoirePosition curNode = positionMap.get(curFen);
        String[] moves = new String[curNode.nextMoves.size()];
        if (moves.length == 0) return "";
        int i = 0;
        for (String move : curNode.nextMoves) {
            moves[i++] = move;
        }

        Random rand = new Random();
        return moves[rand.nextInt(moves.length)];
    }

    // reloads the current repertoire, used after we save a new pgn to our repertoire
    public void reload() throws FileNotFoundException {
        load(path);
    }

    // load a repertoire from a path to the directory
    public void load(String path) throws FileNotFoundException {
        // load new repertoire
        reset();
        // set new path
        this.path = path;
        this.name = path.substring(path.lastIndexOf(File.separator) + 1);
        // import comments from COMMENT_DATA file, create if one doesn't exist
        File commentFile = new File(path + File.separator + "COMMENT_DATA");
        try {
            commentFile.createNewFile(); // only creates new file if none exists with this name
        } catch (IOException e) {
        }
        importCommentData(commentFile);

        // parse each PGN file and add to tree
        // open the directory
        File dir = new File(path);
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File pgnFile : dirFiles) {
                String pgnPath = pgnFile.getPath();
                // only parse if it ends in .pgn
                if (pgnPath.length() < 5 || !pgnPath.endsWith(".pgn"))
                    continue;
                // parse each file
                // System.out.println(f.getPath());
                importPGN(pgnFile);
            }
        }
    }

    public String getCommentAtFen(String fen) {
        TruncatedFen truncFen = new TruncatedFen(fen, false);
        if (commentMap.containsKey(truncFen)) {
            return commentMap.get(truncFen).getComment();
        } else {
//            System.out.println("No Comments at TruncFen: "+ truncFen.toString());
            return "<Your Comments Here>";
        }

    }

    // todo: maybe change these delimiters so that they are more rare or specific so nobody accidentally uses them
    // import comment data from repertoire pointed to by path
    private void importCommentData(File file) throws FileNotFoundException {
        Scanner sc = new Scanner(file);
        sc.useDelimiter("[}][\n][{]");
        while (sc.hasNext()) {
            String commentData = sc.next();
//            System.out.println("token: " + commentData);
            if (commentData.length() <= 1) continue;
            if (commentData.indexOf("{") != -1) commentData = commentData.substring(commentData.indexOf("{") + 1);
            if (commentData.length() <= 1) continue;
            if (commentData.lastIndexOf("}") != -1)
                commentData = commentData.substring(0, commentData.lastIndexOf("}"));
            if (commentData.length() <= 1) continue;
            RepertoireComment temp = new RepertoireComment(commentData.split("[|]"));
            commentMap.put(temp.fen, temp);
        }
    }

    public void saveComment(String fen, String comment) {
        // update the comment in the map
        TruncatedFen truncFen = new TruncatedFen(fen, false);
        if (!commentMap.containsKey(truncFen))
            commentMap.put(truncFen, new RepertoireComment(fen));
        commentMap.get(truncFen).setComment(comment);
        // write the contents of the map to the COMMENT_DATA file
        writeCommentsToFile();
        // now do we need to reload the repertoire? i dont think so
    }

    // writes the contents of commentMap to the COMMENT_DATA file
    private void writeCommentsToFile() {
        try {
            FileWriter writer = new FileWriter(path + File.separator + "COMMENT_DATA");
            for (RepertoireComment val : commentMap.values()) {
                writer.write(val.toString());
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("IOException in Repertoire.writeCommentsToFile()");
        }
    }

    // import the game stored in this pgn to our tree
    private void importPGN(File file) {
        MyFile f = new MyFile(file.getPath());
        String[] lines = f.read_lines();

        // have iterated past header lines
        boolean passed_headers = false;

        // body contains the moves with comments removed
        String body;
        StringBuilder bodyBuilder = new StringBuilder();

        for (String line : lines) {
            if (!passed_headers) {
                // skip headers
                if (line.length() < 2 || line.charAt(0) != '[') {
                    passed_headers = true;
                }
            } else {
                if (line.length() < 2) break; // stop in this case, no more body
                // build body after passing headers
                bodyBuilder.append(line);
                bodyBuilder.append(" ");
            }
        } //todo: update repertoire whenever something is saved to it, like call importPGN on the path in the save as: button code
        body = bodyBuilder.toString(); // get body
        body = body.replaceAll("\r|\n|\\{[^\\}]*\\}", ""); // remove comments and newlines and such
        MyTokenizer moveTokens = new MyTokenizer(body); // tokenize by move
        String nextMove; // iterate over tokens
        Board tempBoard = new Board(false);
        tempBoard.set_from_fen(startingFen);
        TruncatedFen curFen = startingFenTruncated;
        while ((nextMove = moveTokens.get_token()) != null) {
            if (nextMove.charAt(0) >= '0' && nextMove.charAt(0) <= '9') continue; // skip the move numbers (like "1.")
            if (!tempBoard.is_san_move_legal(nextMove))
                break; // stop parsing the rest of the file once an illegal move found
            // update current node
            RepertoirePosition curNode = positionMap.get(curFen);
            curNode.numDescendants++;
            curNode.nextMoves.add(nextMove); // add this move to list of next moves from current position
            // make the move and get the fen from new position
            tempBoard.make_san_move(nextMove, false);
            TruncatedFen nextFen = new TruncatedFen(tempBoard.report_fen(), false);

            // if this is a novel position, add new position to our map
            if (!positionMap.containsKey(nextFen)) {
                positionMap.put(nextFen, new RepertoirePosition(nextFen));
//                System.out.println("novel position: " + nextFen);
            }
            // testing prints
//            System.out.println("processed move: " + nextMove);
//            System.out.println("Current FEN: " + curFen);
//            System.out.println("numDescendants: " + curNode.numDescendants);
//            System.out.print("next moves: ");
//            for (String m : curNode.nextMoves) {
//                System.out.print(m + " ");
//            }
//            System.out.println("\nnew position: " + nextFen);
//            System.out.println("\n\n");
            curFen = nextFen;
        }
    }

}
