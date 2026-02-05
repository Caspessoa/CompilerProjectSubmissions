import ast.*;
import java.util.ArrayList;

public class ArvoreDebug {

    public static void print(Object node) {
        print(node, "");
    }

    private static void print(Object node, String indent) {
        if (node == null) return;

        // --- Estrutura Principal ---
        if (node instanceof Prog) {
            Prog p = (Prog) node;
            System.out.println(indent + "PROGRAMA");
            print(p.main, indent + "  ");
            if (p.fun != null) {
                for (Fun f : p.fun) print(f, indent + "  ");
            }
        } 
        else if (node instanceof Main) {
            Main m = (Main) node;
            System.out.println(indent + "MAIN");
            System.out.println(indent + "  VARIAVEIS:");
            for (VarDecl v : m.vars) print(v, indent + "    ");
            System.out.println(indent + "  COMANDOS:");
            for (Comando c : m.coms) print(c, indent + "    ");
        }
        else if (node instanceof Fun) {
            Fun f = (Fun) node;
            System.out.println(indent + "FUNC " + f.nome + " (Retorno: " + f.retorno + ")");
            System.out.println(indent + "  PARAMS:");
            if (f.params != null) {
                for (ParamFormalFun p : f.params) {
                    System.out.println(indent + "    " + p.type + " " + p.var);
                }
            }
            System.out.println(indent + "  VARIAVEIS:"); 
            if (f.vars != null) {
                for (VarDecl v : f.vars) {
                    print(v, indent + "    ");
                }
            }
            System.out.println(indent + "  COMANDOS:");
            if (f.body != null) {
                for (Comando c : f.body) {
                    print(c, indent + "    ");
                }
            }
        }
        else if (node instanceof VarDecl) {
            VarDecl v = (VarDecl) node;
            System.out.println(indent + "Var: " + v.type + " " + v.var);
        }

        // --- Comandos ---
        else if (node instanceof CAtribuicao) {
            CAtribuicao c = (CAtribuicao) node;
            System.out.print(indent + "Atribuicao: " + c.var + " := ");
            print(c.exp, ""); // Imprime exp na mesma linha (ou adapte conforme gosto)
            System.out.println(); // Quebra linha
        }
        else if (node instanceof CChamadaFun) {
            CChamadaFun c = (CChamadaFun) node;
            System.out.print(indent + "Chamada: " + c.fun + "(");
            for (Exp e : c.args) { print(e, ""); System.out.print(", "); }
            System.out.println(")");
        }
        else if (node instanceof CIf) {
            CIf c = (CIf) node;
            System.out.print(indent + "IF (");
            print(c.exp, "");
            System.out.println(") THEN");
            if (c.bloco != null) for (Comando cmd : c.bloco) print(cmd, indent + "  ");
        }
        else if (node instanceof CWhile) {
            CWhile c = (CWhile) node;
            System.out.print(indent + "WHILE (");
            print(c.exp, "");
            System.out.println(") DO");
            if (c.bloco != null) for (Comando cmd : c.bloco) print(cmd, indent + "  ");
        }
        else if (node instanceof CPrint) {
            CPrint c = (CPrint) node;
            System.out.print(indent + "PRINT ");
            print(c.exp, "");
            System.out.println();
        }
        else if (node instanceof CReturn) {
            CReturn c = (CReturn) node;
            System.out.print(indent + "RETURN ");
            print(c.exp, "");
            System.out.println();
        }

        // --- Expressões (Imprime sem pular linha para ficar fluido) ---
        else if (node instanceof EOpExp) {
            EOpExp e = (EOpExp) node;
            System.out.print("(");
            print(e.arg1, "");
            System.out.print(" " + e.op + " ");
            print(e.arg2, "");
            System.out.print(")");
        }
        else if (node instanceof EFloat) {
            System.out.print(((EFloat) node).value);
        }
        else if (node instanceof EVar) {
            System.out.print(((EVar) node).var);
        }
        else if (node instanceof ETrue) {
            System.out.print("true");
        }
        else if (node instanceof EFalse) {
            System.out.print("false");
        }
        else if (node instanceof EChamadaFun) {
            EChamadaFun e = (EChamadaFun) node;
            System.out.print(e.fun + "(...)");
        }
        else {
            System.out.println(indent + "Nó desconhecido: " + node.getClass().getName());
        }
    }
}