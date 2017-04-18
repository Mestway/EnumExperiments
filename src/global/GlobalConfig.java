package global;

/**
 * Created by clwang on 5/12/16.
 */
public final class GlobalConfig {


    public static final boolean SIMPLIFY_AGGR_FIELD = true;

    // the length of join keys in left-join nodes that will be enumerated
    public static final Integer LEFT_JOIN_KEY_LENGTH = 2;

    public static final Integer MAXIMUM_FILTER_LENGTH = 2;

    // the number of top queries stored during the decoding process
    public static final Integer MAXIMUM_BEAM_SIZE = 10;

    public static final boolean STAT_MODE = false;

    public static final boolean PRINT_LOG = true;

    public static final boolean GUESS_ADDITIONAL_CONSTANTS = false;

    public static int maxSearchDepth = 3;
    public static String firstPhasePruningTech = "Approximation";
    public static int testComplexAggregation = -1;
//  public static String firstPhasePruningTech = "Constraint";

}
