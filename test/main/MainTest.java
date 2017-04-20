package main;

import com.microsoft.z3.*;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by clwang on 4/19/17.
 */
public class MainTest {
    @Test
    public void main() throws Exception {

    }

    @Test
    public void testZ3() throws Exception {
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