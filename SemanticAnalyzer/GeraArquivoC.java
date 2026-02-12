import ast.*;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GeraArquivoC {
    
    private ArrayList<Fun> todasFuncoes;
    private PrintWriter writer;

    public void codegen(Prog prog, String nomeArquivo) {
        this.todasFuncoes = prog.fun;
        
        try {
            FileWriter fileWriter = new FileWriter(nomeArquivo);
            writer = new PrintWriter(fileWriter);
            
            writer.println("#include <stdio.h>");
            writer.println("#include <stdbool.h>");
            writer.println();

            if (prog.fun != null) {
                for (Fun f : prog.fun) genPrototipo(f);
            }
            writer.println();

            genMain(prog.main);
            writer.println();

            if (prog.fun != null) {
                for (Fun f : prog.fun) genFun(f);
            }
            
            writer.close();
            System.out.println("Arquivo '" + nomeArquivo + "' gerado com sucesso!");
            
        } catch (IOException e) {
            System.err.println("Erro ao criar arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void genPrototipo(Fun f) {
        writer.print(mapType(f.retorno) + " " + f.nome + "(");
        genParams(f.params);
        writer.println(");");
    }

    private void genMain(Main m) {
        writer.println("int main() {");
        
        for (VarDecl v : m.vars) {
            writer.println("\t" + mapType(v.type) + " " + v.var + ";");
        }
        
        for (Comando c : m.coms) {
            genComando(c, "\t", m.vars, null);
        }
        
        writer.println("\treturn 0;");
        writer.println("}");
    }

    private void genFun(Fun f) {
        writer.print(mapType(f.retorno) + " " + f.nome + "(");
        genParams(f.params);
        writer.println(") {");
        
        for (VarDecl v : f.vars) {
            writer.println("\t" + mapType(v.type) + " " + v.var + ";");
        }

        for (Comando c : f.body) {
            genComando(c, "\t", f.vars, f.params);
        }

        writer.println("}");
        writer.println();
    }

    private void genParams(ArrayList<ParamFormalFun> params) {
        if (params == null) return;
        for (int i = 0; i < params.size(); i++) {
            ParamFormalFun p = params.get(i);
            writer.print(mapType(p.type) + " " + p.var);
            if (i < params.size() - 1) writer.print(", ");
        }
    }

    private void genComando(Comando c, String indent, ArrayList<VarDecl> vars, ArrayList<ParamFormalFun> params) {
        
        if (c instanceof CAtribuicao) {
            CAtribuicao atr = (CAtribuicao) c;
            writer.print(indent + atr.var + " = ");
            genExp(atr.exp);
            writer.println(";");
        } 
        else if (c instanceof CChamadaFun) {
            CChamadaFun call = (CChamadaFun) c;
            writer.print(indent + call.fun + "(");
            genListaExp(call.args);
            writer.println(");");
        }
        else if (c instanceof CIf) {
            CIf cif = (CIf) c;
            writer.print(indent + "if (");
            genExp(cif.exp);
            writer.println(") {");
            for (Comando cmd : cif.bloco) genComando(cmd, indent + "\t", vars, params);
            writer.println(indent + "}");
        }
        else if (c instanceof CWhile) {
            CWhile cw = (CWhile) c;
            writer.print(indent + "while (");
            genExp(cw.exp);
            writer.println(") {");
            for (Comando cmd : cw.bloco) genComando(cmd, indent + "\t", vars, params);
            writer.println(indent + "}");
        }
        else if (c instanceof CPrint) {
            CPrint cp = (CPrint) c;
            String tipo = inferirTipo(cp.exp, vars, params);

            writer.print(indent + "printf(");
            
            if (tipo.equals("Bool")) {
                writer.print("\"%s\\n\", (");
                genExp(cp.exp);
                writer.print(") ? \"true\" : \"false\"");
            } else {
                writer.print("\"%f\\n\", (float)(");
                genExp(cp.exp);
                writer.print(")");
            }
            writer.println(");");
        }
        // adicionar o read --- NOVO
        else if (c instanceof CReadInput) {
            CReadInput cri = (CReadInput) c;
            String tipo = inferirTipoVariavel(cri.var, vars, params);
            
            writer.print(indent + "scanf(");
            
            // Determina o formato correto baseado no tipo da variável
            if (tipo.equals("Bool") || tipo.equals("Int")) {
                writer.print("\"%d\", &" + cri.var);
            } else if (tipo.equals("Float")) {
                writer.print("\"%f\", &" + cri.var);
            } else {
                // Tipo padrão caso não seja identificado
                writer.print("\"%f\", &" + cri.var);
            }
            writer.println(");");
        }
        else if (c instanceof CReturn) {
            CReturn cr = (CReturn) c;
            writer.print(indent + "return ");
            genExp(cr.exp);
            writer.println(";");
        }
    }

    private void genExp(Exp e) {
        if (e instanceof EOpExp) {
            EOpExp op = (EOpExp) e;
            writer.print("(");
            genExp(op.arg1);
            writer.print(" " + op.op + " ");
            genExp(op.arg2);
            writer.print(")");
        } 
        else if (e instanceof EFloat) {
            writer.print(((EFloat) e).value);
        }
        else if (e instanceof EVar) {
            writer.print(((EVar) e).var);
        }
        else if (e instanceof ETrue) {
            writer.print("1");
        }
        else if (e instanceof EFalse) {
            writer.print("0");
        }
        else if (e instanceof EChamadaFun) {
            EChamadaFun call = (EChamadaFun) e;
            writer.print(call.fun + "(");
            genListaExp(call.args);
            writer.print(")");
        }
    }

    private void genListaExp(ArrayList<Exp> args) {
        if (args == null) return;
        for (int i = 0; i < args.size(); i++) {
            genExp(args.get(i));
            if (i < args.size() - 1) writer.print(", ");
        }
    }

    private String inferirTipo(Exp e, ArrayList<VarDecl> vars, ArrayList<ParamFormalFun> params) {
        if (e instanceof ETrue || e instanceof EFalse) return "Bool";
        if (e instanceof EFloat) return "Float";

        if (e instanceof EOpExp) {
            String op = ((EOpExp) e).op;
            if (op.equals(">") || op.equals("<") || op.equals("==") || 
                op.equals("&&") || op.equals("||")) {
                return "Bool";
            }
            return "Float";
        }

        if (e instanceof EVar) {
            String nomeVar = ((EVar) e).var;
            if (vars != null) {
                for (VarDecl v : vars) {
                    if (v.var.equals(nomeVar)) return v.type;
                }
            }
            if (params != null) {
                for (ParamFormalFun p : params) {
                    if (p.var.equals(nomeVar)) return p.type;
                }
            }
        }

        if (e instanceof EChamadaFun) {
            String nomeFun = ((EChamadaFun) e).fun;
            if (todasFuncoes != null) {
                for (Fun f : todasFuncoes) {
                    if (f.nome.equals(nomeFun)) return f.retorno;
                }
            }
        }

        return "Float";
    }

    //Adicionado 
    // Percorre as declarações de variáveis locais e parâmetros para encontrar o tipo
    private String inferirTipoVariavel(String nomeVar, ArrayList<VarDecl> vars, ArrayList<ParamFormalFun> params) {
        // Busca nas variáveis locais
        if (vars != null) {
            for (VarDecl v : vars) {
                if (v.var.equals(nomeVar)) return v.type;
            }
        }
        
        // Busca nos parâmetros da função
        if (params != null) {
            for (ParamFormalFun p : params) {
                if (p.var.equals(nomeVar)) return p.type;
            }
        }
        
        // Tipo padrão caso não encontre
        return "Float";
    }

    private String mapType(String type) {
        if (type.equals("Float")) return "float";
        if (type.equals("Bool")) return "int";
        if (type.equals("Void")) return "void";
        return "int";
    }
}