package lang.sql.ast.val;

import lang.sql.ast.Environment;
import lang.sql.datatype.ValType;
import lang.sql.datatype.Value;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.trans.ValNodeSubstBinding;

import java.util.List;

/**
 * Cast a constant value to a name
 * Created by clwang on 12/17/15.
 */
public class AsNamedVal implements ValNode {
    ValNode valNode;
    String name;

    public AsNamedVal(ValNode valNode, String name) {
        this.valNode = valNode;
        this.name = name;
    }

    @Override
    public Value eval(Environment env) throws SQLEvalException {
        return valNode.eval(env);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ValType getType(EnumContext ctxt) {
        return valNode.getType(ctxt);
    }

    @Override
    public String prettyPrint(int Lv) {
        return valNode.toString() + " AS " + name;
    }

    @Override
    public int getNestedQueryLevel() {
        return valNode.getNestedQueryLevel();
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        if (vn instanceof AsNamedVal) {
            if (((AsNamedVal) vn).valNode.equalsToValNode(valNode))
                return true;
        }
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return valNode.getAllHoles();
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return new AsNamedVal(valNode.instantiate(env), this.name);
    }

    @Override
    public ValNode subst(ValNodeSubstBinding vnsb) {
        return new AsNamedVal(valNode.subst(vnsb), this.name);
    }

}
