// Liam Phelan 17451926
// Hugh McKeeney 17324636
// Hannah O'Dea 17405444

public class LHH implements BotAPI
{
    private PlayerAPI me, opponent;
    private BoardAPI board;
    private CubeAPI cube;
    private MatchAPI match;
    private InfoPanelAPI info;

    private static  final int BEAR_OFF = 0;
    private static  final int BAR = 25;

    private int[][] boardLayoutWithMoveApplied;

    LHH (PlayerAPI me, PlayerAPI opponent, BoardAPI board, CubeAPI cube, MatchAPI match, InfoPanelAPI info)
    {

        this.me = me;
        this.opponent = opponent;
        this.board = board;
        this.cube = cube;
        this.match = match;
        this.info = info;
    }

    @Override
    public String getName() { return "LHH"; }

    @Override
    public String getCommand(Plays possiblePlays)
    {
        double[] weightedPlayScores = new double[possiblePlays.number()];

        for (int currentPlay = 0; currentPlay < possiblePlays.number(); currentPlay++)
        {
            boardLayoutWithMoveApplied = board.get();
            applyMoveToBoard(possiblePlays.get(currentPlay));

            int pipCountMe = getPipCount(me.getId());
            int pipCountOpponent = getPipCount(opponent.getId());
            int pipCountDifference = getPipCountDifference();
            int blockBlotDifference = getBlockBlotDifference();
            int homeBoardBlockCount = getHomeBoardBlockCount();

            int numCheckersOffMe = getNumCheckersOffMe();
            int numCheckersOffOpponent = getNumCheckersOffOpponent();

            int numPointsCoveredMe = getNumPointsCoveredMe();
            int numPointsCoveredOpponent = getNumPointsCoveredOpponent();

            int numCheckersInHomeBoardMe = getNumCheckersInHomeBoardMe();
            int numCheckersInHomeBoardOpponent = getNumCheckersInHomeBoardOpponent();

            int numEscapedCheckersMe = getNumEscapedCheckersMe();
            int numEscapedCheckersOpponent = getNumEscapedCheckersOpponent();

            int numHits = getHitCount();
            int numBearOff = isBearOff();

            boolean meAtBeginning = numEscapedCheckersMe < 8;
            boolean opponentAtBeginning = numEscapedCheckersOpponent < 8;

            boolean meInMiddle = numEscapedCheckersMe >= 8;
            boolean opponentInMiddle = numEscapedCheckersOpponent >= 8;

            boolean meNearEnd = pipCountMe <= 100 && numEscapedCheckersMe >= 10;
            boolean opponentNearEnd = pipCountOpponent <= 100 && numEscapedCheckersOpponent >= 10;

            boolean meBearOff = numCheckersInHomeBoardMe + numCheckersOffMe == 15;
            boolean opponentBearOff = numCheckersInHomeBoardOpponent + numCheckersOffOpponent == 15;

            if(meBearOff)
            {
                if(numEscapedCheckersOpponent != 15)
                {
                    weightedPlayScores[currentPlay] = numBearOff + numHits * 4;
                }
                else
                {
                    weightedPlayScores[currentPlay] = numBearOff;
                }
            }
            else if(meNearEnd)
            {
                if(numEscapedCheckersOpponent != 15)
                    weightedPlayScores[currentPlay] = numHits;
            }
            else if(meInMiddle)
            {
                if(opponentAtBeginning)
                {
                    weightedPlayScores[currentPlay] = (numHits*10 + (getBlockCount(me.getId())*5) + (getLongestPrime(me.getId()) * 4) - (getBlotCount(me.getId()))*100);
                }
                else if(opponentInMiddle)
                {
                    weightedPlayScores[currentPlay] = (numHits*40 + homeBoardBlockCount*2 + numPointsCoveredMe*1.5 + (getBlockCount(me.getId()))*3 + (getLongestPrime(me.getId()))*3 - (getBlotCount(me.getId()))*100);
                }
                else if(opponentNearEnd)
                {
                    weightedPlayScores[currentPlay] = (numHits*15 + numCheckersOffMe*20 + numCheckersInHomeBoardMe*10 - (getBlotCount(me.getId()))*100);
                }

            }
            else if (meAtBeginning)
            {
                // Try to move forward as quickly as possible, and aggressively hit opponent checkers.
                if (opponentNearEnd)
                {
                    weightedPlayScores[currentPlay] = (getPipCount(me.getId()) * -1) + (numHits * 5);
                }

                // Try to move forward fairly quickly, hit opponent, and create some blocks.
                else if (opponentInMiddle)
                {
                    weightedPlayScores[currentPlay] = (getBlockCount(me.getId()) * 0.5) + (getPipCount(me.getId()) * -0.5) + numHits;
                }

                // Try to create blocks, heavily avoid blots, hit opponent, and move forward with less priority than the other two scenarios.
                else if (opponentAtBeginning)
                {
                    weightedPlayScores[currentPlay] = getBlockCount(me.getId()) + (getPipCount(me.getId()) * -0.25) - (getBlotCount(me.getId()) * 100) + numHits;
                }
            }
            currentPlay++;
        }

        return Integer.toString(bestPlay(weightedPlayScores));
    }

    @Override
    public String getDoubleDecision()
    {
        boolean meAtBeginning = getNumEscapedCheckersMe() < 8;
        boolean meInMiddle = getNumEscapedCheckersMe() >= 8;
        boolean opponentNearEnd = getNumCheckersInHomeBoardOpponent() > 12;
        String decision;

        if ( (meAtBeginning && opponentNearEnd) || (meInMiddle && opponentNearEnd))
            decision = "n";
        else
            decision = "y";

        return decision;
    }

    private void applyMoveToBoard(Play possiblePlay)
    {
        for (int i = 0; i < possiblePlay.numberOfMoves(); i++)
        {
            int sourcePip = possiblePlay.getMove(i).getFromPip();
            int destinationPip = possiblePlay.getMove(i).getToPip();

            boardLayoutWithMoveApplied[me.getId()][sourcePip]--;
            boardLayoutWithMoveApplied[me.getId()][destinationPip]++;

            if (possiblePlay.getMove(i).isHit())
                boardLayoutWithMoveApplied[opponent.getId()][destinationPip]--;
        }
    }

    private int bestPlay(double[] playScores)
    {
        int bestPlay = 0;

        for (int i = 0; i < playScores.length; i++)
        {
            if (playScores[i] > playScores[bestPlay])
                bestPlay = i;
        }

        return bestPlay + 1;
    }

    //--------- Code for methods calculating bot decision criteria ---------

    //---- Finding longest prime ----

    private int getLongestPrime(int playerID)
    {
        int longestPrime = 0;

        for (int currentPip = 24, currentPrime = 0; currentPip > BEAR_OFF; currentPip--)
        {
            if ( isBlock( getNumCheckersWithMoveApplied(playerID, currentPip) ) )
            {
                currentPrime++;
            }
            else
            {
                if (currentPrime > longestPrime)
                    longestPrime = currentPrime;
                currentPrime = 0;
            }
        }

        return longestPrime;
    }

    //---- Calculating the pip count difference ----

    private int getPipCountDifference() { return  getPipCount(me.getId()) - getPipCount(opponent.getId()); }

    private int getPipCount(int playerID)
    {
        int pipCount = 0;

        for (int pipNum = BAR; pipNum > BEAR_OFF; pipNum--)
            pipCount += getPipCountValueOnSinglePip(playerID, pipNum);

        return  pipCount;
    }

    private int getPipCountValueOnSinglePip(int playerID, int pipNum)
    {
        return board.getNumCheckers(playerID, pipNum) * pipNum;
    }

    //---- Calculating the block-blot difference & home board block count ----

    private int getBlockBlotDifference()
    {
        int blotCount = getBlotCount(me.getId());
        int blockCount = getBlockCount(opponent.getId());

        return blotCount - blockCount;
    }

    private int getBlotCount(int playerID)
    {
        int numBlots = 0;

        for (int pipNum = 24; pipNum > 0; pipNum--)
            if (isBlot(getNumCheckersWithMoveApplied(playerID, pipNum))) { numBlots++; }

        return numBlots;
    }

    private int getBlockCount(int playerID)
    {
        int numBlocks = 0;

        for (int pipNum = 24; pipNum > 0; pipNum--)
            if (isBlock(getNumCheckersWithMoveApplied(playerID, pipNum))) { numBlocks++; }

        return numBlocks;
    }

    private int getHomeBoardBlockCount()
    {
        int numBlocks = 0;

        for (int pipNum = 6; pipNum > 0; pipNum--)
            if (isBlock(getNumCheckersWithMoveApplied(me.getId(), pipNum))) { numBlocks++; }

        return numBlocks;
    }

    private boolean isBlot(int numCheckersOnPip) { return (numCheckersOnPip == 1); }

    private boolean isBlock(int numCheckersOnPip) { return (numCheckersOnPip > 1); }

    private int getNumCheckersWithMoveApplied(int playerID, int pipNum) { return boardLayoutWithMoveApplied[playerID][pipNum]; }

    private int getNumCheckersOffMe()
    {
        return board.getNumCheckers(me.getId(), 0);
    }

    private int getNumCheckersOffOpponent()
    {
        return board.getNumCheckers(opponent.getId(),0);
    }

    private int getNumPointsCoveredMe()
    {
        int count = 0;
        for(int i = 1; i <= 24; i++)
        {
            if(board.getNumCheckers(me.getId(), i) > 0)
            {
                count++;
            }
        }
        return count;
    }

    private int getNumPointsCoveredOpponent()
    {
        int count = 0;
        for(int i = 1; i <= 24; i++)
        {
            if(board.getNumCheckers(opponent.getId(), i) > 0)
            {
                count++;
            }
        }
        return count;
    }

    private int getNumCheckersInHomeBoardMe()
    {
        int count = 0;
        for(int pipNum = 1; pipNum <= 6; pipNum++)
        {
            if(board.getNumCheckers(me.getId(), pipNum) > 0)
            {
                count = count + board.getNumCheckers(me.getId(), pipNum);
            }
        }

        return count;
    }

    private int getNumCheckersInHomeBoardOpponent()
    {
        int count = 0;
        for(int pipNum = 1; pipNum <= 6; pipNum++)
        {
            if(board.getNumCheckers(opponent.getId(), pipNum) > 0)
            {
                count = count + board.getNumCheckers(opponent.getId(), pipNum);
            }
        }

        return count;
    }

    private int getNumEscapedCheckersMe()
    {
        int count = 0;
        for(int pipNum = 0; pipNum <= 25; pipNum++)
        {
            if( pipNum > (24 - getFurthestChecker(opponent.getId()) + 1)   )
                if(board.getNumCheckers(me.getId(), pipNum) > 0)
                    count = count + board.getNumCheckers(me.getId(), pipNum);
        }
        return count;
    }

    private int getNumEscapedCheckersOpponent()
    {
        int count = 0;
        for(int pipNum = 0; pipNum <= 25; pipNum++)
        {
            if( pipNum > (24 - getFurthestChecker(me.getId()) + 1)   )
                if(board.getNumCheckers(opponent.getId(), pipNum) > 0)
                    count = count + board.getNumCheckers(me.getId(), pipNum);
        }
        return count;
    }

    private int getFurthestChecker(int Id)
    {
        int lastChecker = 1;
        for(int pipNum = 1; pipNum <25; pipNum++)
        {
            if(board.getNumCheckers(Id, pipNum ) > 0)
                lastChecker = pipNum;
        }
        return lastChecker;
    }

    private int getHitCount()
    {
        if(board.getNumCheckers(me.getId(), 25) == getNumCheckersWithMoveApplied(me.getId(), 25) + 1 )
            return 1;
        else if(board.getNumCheckers(me.getId(), 25) == getNumCheckersWithMoveApplied(me.getId(), 25) + 2)
            return 2;
        else
            return 0;
    }

    private int isBearOff()
    {
        if(board.getNumCheckers(me.getId(), 0)  + 1 == getNumCheckersWithMoveApplied(me.getId(), 0) )
            return 1;
        else if(board.getNumCheckers(me.getId(), 0)  + 2 == getNumCheckersWithMoveApplied(me.getId(), 0) )
            return 2;
        else if(board.getNumCheckers(me.getId(), 0)  + 3 == getNumCheckersWithMoveApplied(me.getId(), 0) )
            return 3;
        else if(board.getNumCheckers(me.getId(), 0)  + 4 == getNumCheckersWithMoveApplied(me.getId(), 0) )
            return 4;
        else
            return 0;
    }
}
