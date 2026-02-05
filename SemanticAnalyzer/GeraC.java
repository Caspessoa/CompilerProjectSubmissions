import ast.*;
import java.util.ArrayList;

/**
 * A ideia é percorrer a árvore (que já está validada sintaticamente) 
 * e escrever os comandos equivalentes na sintaxe de C.
 */
public class GeraC {
    
    // Guardamos a lista de todas as funções do programa aqui.
    // Isso é vital para quando acharmos uma chamada de função (ex: calcular())
    // sabermos qual é o tipo que ela retorna (Float, Bool, etc).
    private ArrayList<Fun> todasFuncoes;

    public void codegen(Prog prog) {
        this.todasFuncoes = prog.fun; // Salva o contexto global

        // 1. Cabeçalhos padrão do C
        System.out.println("#include <stdio.h>");
        System.out.println("#include <stdbool.h>"); // Permite usar bool/true/false se quisermos
        System.out.println("");

        // 2. Protótipos das Funções (Forward Declaration)
        // C sendo sequencial, se a main chamar uma função que está escrita lá embaixo,
        // o compilador reclama
        if (prog.fun != null) {
            for (Fun f : prog.fun) genPrototipo(f);
        }
        System.out.println("");

        // 3. Geramos a função Main
        genMain(prog.main);
        System.out.println("");

        // 4. Implementação real das Funções
        if (prog.fun != null) {
            for (Fun f : prog.fun) genFun(f);
        }
    }

    // Apenas a assinatura da função: float minhaFuncao(int a);
    private void genPrototipo(Fun f) {
        System.out.print(mapType(f.retorno) + " " + f.nome + "(");
        genParams(f.params);
        System.out.println(");");
    }

    private void genMain(Main m) {
        System.out.println("int main() {");
        
        // Em C antigo (ANSI), variáveis vinham no topo. É uma boa prática manter assim.
        for (VarDecl v : m.vars) {
            System.out.println("\t" + mapType(v.type) + " " + v.var + ";");
        }
        
        // Aqui passamos as variáveis locais 'm.vars' para o gerador de comandos.
        // Precisamos delas para saber o tipo de cada variável na hora de dar print.
        for (Comando c : m.coms) {
            genComando(c, "\t", m.vars, null); // Main não tem parâmetros, então passamos null
        }
        
        System.out.println("\treturn 0;");
        System.out.println("}");
    }

    private void genFun(Fun f) {
        System.out.print(mapType(f.retorno) + " " + f.nome + "(");
        genParams(f.params);
        System.out.println(") {");
        
        // Declara variáveis locais da função
        for (VarDecl v : f.vars) {
            System.out.println("\t" + mapType(v.type) + " " + v.var + ";");
        }

        // Gera o corpo. Note que passamos 'f.vars' (locais) E 'f.params' (argumentos).
        // Se o código usar 'x', precisamos procurar nessas duas listas para saber o que é 'x'.
        for (Comando c : f.body) {
            genComando(c, "\t", f.vars, f.params);
        }

        System.out.println("}");
        System.out.println("");
    }

    // Gera: float a, int b, ...
    private void genParams(ArrayList<ParamFormalFun> params) {
        if (params == null) return;
        for (int i = 0; i < params.size(); i++) {
            ParamFormalFun p = params.get(i);
            System.out.print(mapType(p.type) + " " + p.var);
            if (i < params.size() - 1) System.out.print(", ");
        }
    }

    
    // Este método decide como escrever cada comando (If, While, Print, etc)
    private void genComando(Comando c, String indent, ArrayList<VarDecl> vars, ArrayList<ParamFormalFun> params) {
        
        if (c instanceof CAtribuicao) {
            CAtribuicao atr = (CAtribuicao) c;
            System.out.print(indent + atr.var + " = ");
            genExp(atr.exp); // Resolve a expressão do lado direito
            System.out.println(";");
        } 
        else if (c instanceof CChamadaFun) {
            CChamadaFun call = (CChamadaFun) c;
            System.out.print(indent + call.fun + "(");
            genListaExp(call.args);
            System.out.println(");");
        }
        else if (c instanceof CIf) {
            CIf cif = (CIf) c;
            System.out.print(indent + "if (");
            genExp(cif.exp);
            System.out.println(") {");
            // Recursão: gera os comandos de dentro do bloco IF
            for (Comando cmd : cif.bloco) genComando(cmd, indent + "\t", vars, params);
            System.out.println(indent + "}");
        }
        else if (c instanceof CWhile) {
            CWhile cw = (CWhile) c;
            System.out.print(indent + "while (");
            genExp(cw.exp);
            System.out.println(") {");
            for (Comando cmd : cw.bloco) genComando(cmd, indent + "\t", vars, params);
            System.out.println(indent + "}");
        }
        else if (c instanceof CPrint) {
            CPrint cp = (CPrint) c;
            
        
            // O C precisa saber se imprimimos %f (float) ou string (bool).
            // Chamamos 'inferirTipo' para descobrir o que estamos imprimindo.
            String tipo = inferirTipo(cp.exp, vars, params);

            System.out.print(indent + "printf(");
            
            if (tipo.equals("Bool")) {
                // Em C, bool é 0 ou 1. 
                // Usamos um operador ternário ( ? : ) para imprimir "true" ou "false" na tela.
                System.out.print("\"%s\\n\", (");
                genExp(cp.exp);
                System.out.print(") ? \"true\" : \"false\"");
            } else {
                // Caso padrão (Float). O cast (float) garante segurança.
                System.out.print("\"%f\\n\", (float)("); 
                genExp(cp.exp);
                System.out.print(")");
            }
            System.out.println(");");
        }
        else if (c instanceof CReturn) {
            CReturn cr = (CReturn) c;
            System.out.print(indent + "return ");
            genExp(cr.exp);
            System.out.println(";");
        }
    }

    // Gera o código das expressões recursivamente (ex: (a + b) * c )
    private void genExp(Exp e) {
        if (e instanceof EOpExp) {
            EOpExp op = (EOpExp) e;
            System.out.print("(");
            genExp(op.arg1);
            // Sorte nossa: os operadores (+, -, *, &&) são iguais em Java/Lovelace e C!
            System.out.print(" " + op.op + " ");
            genExp(op.arg2);
            System.out.print(")");
        } 
        else if (e instanceof EFloat) {
            System.out.print(((EFloat) e).value);
        }
        else if (e instanceof EVar) {
            System.out.print(((EVar) e).var);
        }
        else if (e instanceof ETrue) {
            System.out.print("1"); // Traduzindo: Lovelace 'true' vira C '1'
        }
        else if (e instanceof EFalse) {
            System.out.print("0"); // Traduzindo: Lovelace 'false' vira C '0'
        }
        else if (e instanceof EChamadaFun) {
            EChamadaFun call = (EChamadaFun) e;
            System.out.print(call.fun + "(");
            genListaExp(call.args);
            System.out.print(")");
        }
    }

    private void genListaExp(ArrayList<Exp> args) {
        if (args == null) return;
        for (int i = 0; i < args.size(); i++) {
            genExp(args.get(i));
            if (i < args.size() - 1) System.out.print(", ");
        }
    }

    // --- INFERÊNCIA DE TIPOS 
    // Precisamos descobrir o tipo de uma expressão para usar o printf correto.
    private String inferirTipo(Exp e, ArrayList<VarDecl> vars, ArrayList<ParamFormalFun> params) {
        // 1. O mais óbvio: literais
        if (e instanceof ETrue || e instanceof EFalse) return "Bool";
        if (e instanceof EFloat) return "Float";

        // 2. Operações: Se tem >, <, == ou &&, o resultado final é sempre Bool
        if (e instanceof EOpExp) {
            String op = ((EOpExp) e).op;
            if (op.equals(">") || op.equals("<") || op.equals("==") || 
                op.equals("&&") || op.equals("||")) {
                return "Bool";
            }
            return "Float"; // Soma, subtração, etc, retornam Float
        }

        // 3. Variáveis: Precisamos procurar onde ela foi declarada.
        if (e instanceof EVar) {
            String nomeVar = ((EVar) e).var;
            // É uma variável local?
            if (vars != null) {
                for (VarDecl v : vars) {
                    if (v.var.equals(nomeVar)) return v.type;
                }
            }
            // É um parâmetro da função?
            if (params != null) {
                for (ParamFormalFun p : params) {
                    if (p.var.equals(nomeVar)) return p.type;
                }
            }
        }

        // 4. Chamada de Função: Qual tipo a função retorna?
        if (e instanceof EChamadaFun) {
            String nomeFun = ((EChamadaFun) e).fun;
            if (todasFuncoes != null) {
                for (Fun f : todasFuncoes) {
                    if (f.nome.equals(nomeFun)) return f.retorno;
                }
            }
        }

        return "Float"; // Padrão: se não sabemos, assumimos Float 
    }

    // Dicionário de tradução de tipos
    private String mapType(String type) {
        if (type.equals("Float")) return "float";
        if (type.equals("Bool")) return "int"; // C não tem tipo nativo Bool, por isso o int
        if (type.equals("Void")) return "void";
        return "int";
    }
}