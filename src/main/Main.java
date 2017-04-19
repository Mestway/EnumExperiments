package main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import forward_enumeration.enum_abstract.AbsQueryEnumerator;

import static main.Synthesizer.Synthesize;
import com.microsoft.z3.*;
import global.GlobalConfig;

import java.util.List;

public class Main {

    @Parameter( arity = 1, description = "Target File", required = true)
    private List<String> targetFile;

    @Parameter(names={"--pruneWithConstraint", "-c"})
    private boolean pruneWithConstraint;

    @Parameter(names={"--pruneWithApproximation", "-a"})
    private boolean pruneWithApproximation = true;

    @Parameter(names={"--complex-aggr-depth"})
    private int testComplexAggr = -1;

    @Parameter(names={"--depth", "-d"})
    private int searchDepth = -1;

    @Parameter(names={"--output"})
    private String outputDir = null;

    public static void main(String[] args) {

        Main main = new Main();
        JCommander commander = new JCommander();
        commander.addObject(main);
        commander.parse(args);

        main.run();

        //testZ3();
    }

    public void run() {

        if (pruneWithConstraint == true)
            GlobalConfig.firstPhasePruningTech = "Constraint";
        else GlobalConfig.firstPhasePruningTech = "Approximation";

        if (searchDepth > 0)
            GlobalConfig.maxSearchDepth = searchDepth;

        if (testComplexAggr > 0)
            GlobalConfig.testComplexAggregation = testComplexAggr;

        System.out.println("[Start Testing] " + targetFile + " "
                + GlobalConfig.firstPhasePruningTech
                + " depth:" + GlobalConfig.maxSearchDepth);
        Synthesize(targetFile.get(0), new AbsQueryEnumerator());
    }

    public static void testZ3()
    {
        {
            Context ctx = new Context();
            /* do something with the context */
            unsatCoreAndProofExample2(ctx);
            /* be kind to dispose manually and not wait for the GC. */
            ctx.close();
        }
    }

    public static void unsatCoreAndProofExample2(Context ctx) {

        System.out.println("UnsatCoreAndProofExample2");

        Solver solver = ctx.mkSolver();

        BoolExpr pa = ctx.mkBoolConst("PredA");
        BoolExpr pb = ctx.mkBoolConst("PredB");
        BoolExpr pc = ctx.mkBoolConst("PredC");
        BoolExpr pd = ctx.mkBoolConst("PredD");

        BoolExpr f1 = ctx.mkAnd(new BoolExpr[] { pa, pb, pc });
        BoolExpr f2 = ctx.mkAnd(new BoolExpr[] { pa, ctx.mkNot(pb), pc });
        BoolExpr f3 = ctx.mkOr(ctx.mkNot(pa), ctx.mkNot(pc));
        BoolExpr f4 = pd;

        BoolExpr p1 = ctx.mkBoolConst("P1");
        BoolExpr p2 = ctx.mkBoolConst("P2");
        BoolExpr p3 = ctx.mkBoolConst("P3");
        BoolExpr p4 = ctx.mkBoolConst("P4");

        solver.assertAndTrack(f1, p1);
        solver.assertAndTrack(f2, p2);
        solver.assertAndTrack(f3, p3);
        solver.assertAndTrack(f4, p4);
        Status result = solver.check();

        if (result == Status.UNSATISFIABLE)
        {
            System.out.println("unsat");
            System.out.println("core: ");
            for (Expr c : solver.getUnsatCore())
            {
                System.out.println(c);
            }
        }

    }
}
