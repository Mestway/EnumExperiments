package util;

import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.ast.val.ValNode;
import lang.sql.exception.SQLEvalException;

import java.util.List;

/**
 * Created by clwang on 1/7/16.
 */
public class DebugHelper {


    public static boolean debugFlag = false;

    public static void printTableNodes(List<AbsTableNode> tns) {
        for (AbsTableNode tn  : tns) {
            try {
                System.out.println("========================");
                System.out.println(tn.prettyPrint(0, false));
                Table tb = tn.eval(new Environment());
                System.out.println(tb);
            } catch (SQLEvalException e) {
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println(tn.prettyPrint(0, false));;
            }
        }
    }

    public static void printList(List l) {
        for (Object t : l) {
            System.out.println("--------------------------");
            System.out.println(t.toString());
        }
    }

    public static void printListHorizontal(List l) {
        String result = "";
        for (Object o : l) {
            result += o.toString();
        }
        System.out.println(result);
    }

    public static void printValNodes(List<ValNode> vns) {
        for (ValNode vn : vns) {
            System.out.println("---------------------------");
            System.out.println(vn.prettyPrint(0));
        }
    }
}
