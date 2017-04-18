package lang.sql.ast.val;

import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.datatype.ValType;
import lang.sql.datatype.Value;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import util.IndentionManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * A value which is a constant
 * Created by clwang on 12/16/15.
 */
public class ConstantVal implements ValNode {
    Value val;

    public ConstantVal(Value val) {
        this.val = val;
    }

    public void setVal(Value val) {
        this.val = val;
    }

    public Value eval(Environment env) throws SQLEvalException {
        return val;
    }

    public Value getValue() { return this.val; }

    @Override
    public String getName() {
        return "anonymous";
    }

    @Override
    public ValType getType(EnumContext ctxt) {
        return val.getValType();
    }

    @Override
    public String prettyPrint(int lv) {
        String formatVal = val.toString();
        if (val.getValType().equals(ValType.StringVal))
            formatVal = "'" + formatVal + "'";
        return IndentionManagement.addIndention(formatVal, lv);
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        if (vn instanceof ConstantVal) {
            if (this.val.equals(((ConstantVal) vn).val))
                return true;
        }
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return new ArrayList<>();
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return this;
    }

    @Override
    public ValNode subst(ValNodeSubstBinding vnsb) {
        return this;
    }
}
