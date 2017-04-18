package lang.table;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Expr;

/**
 * Created by clwang on 4/13/17.
 */
public class TableAttr {
    public ArithExpr r;
    public ArithExpr c;
    public ArithExpr newVal;

    public TableAttr(ArithExpr r, ArithExpr c, ArithExpr newVal) {
        this.r = r;
        this.c = c;
        this.newVal = newVal;
    }
}
