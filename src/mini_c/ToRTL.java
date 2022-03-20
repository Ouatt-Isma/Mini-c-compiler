package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

class ToRTL extends EmptyVisitor {

  private RTLfile rtlfile;

  //RTL result of the last visit
  RTLgraph current_graph; //RTLgraph of the current function we are visit

  Stack<RTLgraph> stack_graph;
  RTLfun fun; //the current function
  Label last_result; // Label result of the last visit
  // HashMap<String, Register> var_locals = new HashMap<>();
  Stack<HashMap<String, Register>> stack_var_locals = new Stack<>();
  boolean first_block = true;
  HashMap<String, Register> var_locals;
  /*
  When visiting an expression we need the Label where we should go after, and the 
  register where to put the result
  here stack_reg will give us this register while stack_lab give us the Label
  */

  public Stack<Register> stack_reg;
  public Stack<Label> stack_lab;

  Stack<Register> reg_retour = new Stack<>();

  //   public boolean debug = false;

  RTLfile getRTLFile() {
    if (rtlfile == null) throw new Error("typing not yet done!");
    return rtlfile;
  }

  RTLfile translate(File f) {
    this.visit(f);
    return rtlfile;
  }

  public void visit(File f) {
    reg_retour.push(null);
    // if (debug) System.out.println("File Deb");
    rtlfile = new RTLfile();
    for (Decl_fun d : f.funs) {
      this.visit(d);
    }
    // if (debug) System.out.println("File fin");
  }

  public void visit(Decl_fun d) {
    // if (debug)
    // System.out.println("Decl_fun Deb" + d.fun_name);
    stack_reg = new Stack<>();
    stack_lab = new Stack<>();
    RTLfun tmp = new RTLfun(d.fun_name);
    fun = tmp;
    tmp.exit = new Label();
    stack_var_locals = new Stack<>();
    Register r;
    HashMap<String, Register> fun_locals;
    fun_locals = new HashMap<>();
    for (Decl_var v : d.fun_formals) {
      r = new Register();
      tmp.formals.add(r);
      fun_locals.put(v.name, r);
    }

    tmp.result = new Register();
    tmp.body = new RTLgraph();
    current_graph = tmp.body;

    var_locals = fun_locals;
    stack_var_locals.push(fun_locals);
    first_block = true;
    this.visit(d.fun_body);

    tmp.entry = last_result;
    tmp.body = current_graph;
    rtlfile.funs.add(tmp);
    // if (debug) System.out.println("Decl_fun Fin");
  }

  public void visit(Stmt stmt) {
    // if (debug) System.out.println("Stmt Deb");
    if (stmt instanceof Sskip) {
      this.visit((Sskip) stmt);
    }
    if (stmt instanceof Sexpr) {
      this.visit((Sexpr) stmt);
    }
    if (stmt instanceof Sif) {
      this.visit((Sif) stmt);
    }
    if (stmt instanceof Sblock) {
      this.visit((Sblock) stmt);
    }
    if (stmt instanceof Sreturn) {
      this.visit((Sreturn) stmt);
    }
    if (stmt instanceof Swhile) {
      this.visit((Swhile) stmt);
    }
    // if (debug) System.out.println("Stmt Fin");
  }

  public void visit(Sblock sb) {
    // if (debug) System.out.println("Sblock Deb");
    Register r;
    if (!first_block) {
      var_locals = new HashMap<>();
      for (Decl_var v : sb.dl) {
        r = new Register();
        var_locals.put(v.name, r);
      }
      HashMap<String, Register> prev = stack_var_locals.peek();
      for (String s : prev.keySet()) {
        if (!var_locals.containsKey(s)) var_locals.put(s, prev.get(s));
      }
    } else {
      first_block = false;
      var_locals = stack_var_locals.pop();
      for (Decl_var v : sb.dl) {
        r = new Register();
        var_locals.put(v.name, r);
        fun.locals.add(r);
      }
    }
    stack_var_locals.push(var_locals);
    int ss = sb.sl.size();
    ListIterator<Stmt> iterator = sb.sl.listIterator(ss);

    // reverse direction
    while (iterator.hasPrevious()) {
      this.visit(iterator.previous());
    }
    for (Decl_var v : sb.dl) {
      r = var_locals.get(v.name);
    }

    if (stack_var_locals.size() != 1) {
      stack_var_locals.pop();
      var_locals = stack_var_locals.peek();
    }
    // if (debug) System.out.println("Sblock Fin");
    return;
  }

  public void visit(Sreturn se) {
    // if (debug) System.out.println("Sreturn Deb");
    stack_lab.push(fun.exit);
    stack_reg.push(fun.result);
    this.visit(se.e);
    // if (debug) System.out.println("Sreturn fin");
    return;
  }

  public void visit(Expr e) {
    // if (debug) System.out.println("Expr Deb");
    if (e instanceof Econst) {
      this.visit((Econst) e);
    } else if (e instanceof Ebinop) {
      this.visit((Ebinop) e);
    } else if (e instanceof Eaccess_local) {
      this.visit((Eaccess_local) e);
    } else if (e instanceof Eunop) {
      this.visit((Eunop) e);
    } else if (e instanceof Eassign_local) {
      this.visit((Eassign_local) e);
    } else if (e instanceof Ecall) {
      this.visit((Ecall) e);
    } else if (e instanceof Eaccess_field) {
      this.visit((Eaccess_field) e);
    } else if (e instanceof Eassign_field) {
      this.visit((Eassign_field) e);
    } else if (e instanceof Esizeof) {
      this.visit((Esizeof) e);
    }
    stack_reg.push(new Register());
    stack_lab.push(last_result);
    // if (debug) System.out.println("Expr fin");
  }

  public void visit(Econst n) {
    // if (debug) System.out.println("Econst Deb");
    Register rd = stack_reg.pop();
    Label Ld = stack_lab.pop();
    Label L1 = new Label();
    current_graph.graph.put(L1, new Rconst(n.i, rd, Ld));
    last_result = L1;
    // if (debug) System.out.println("Econst Fin");
  }

  public Expr reduction(Expr nexpr) {
    // if (debug) System.out.println("Deb red");
    if (nexpr instanceof Ebinop) {
      Ebinop n = (Ebinop) nexpr;
      if (
        ((Ebinop) nexpr).e1 instanceof Econst &&
        ((Ebinop) nexpr).e2 instanceof Econst
      ) {
        int result = 0;
        if (n.b != Binop.Band && n.b != Binop.Bor) {
          if (n.b == Binop.Badd) result = ((Econst) n.e1).i + ((Econst) n.e2).i;
          if (n.b == Binop.Bsub) result = ((Econst) n.e1).i - ((Econst) n.e2).i;
          if (n.b == Binop.Bmul) result = ((Econst) n.e1).i * ((Econst) n.e2).i;
          if (n.b == Binop.Bdiv) {
            if (((Econst) n.e2).i == 0) return nexpr;
            result = ((Econst) n.e1).i / ((Econst) n.e2).i;
          }
          if (n.b == Binop.Beq) result =
            ((Econst) n.e1).i == ((Econst) n.e2).i ? 1 : 0;
          if (n.b == Binop.Bneq) result =
            ((Econst) n.e1).i != ((Econst) n.e2).i ? 1 : 0;
          if (n.b == Binop.Bgt) result =
            ((Econst) n.e1).i > ((Econst) n.e2).i ? 1 : 0;
          if (n.b == Binop.Bge) result =
            ((Econst) n.e1).i >= ((Econst) n.e2).i ? 1 : 0;
          if (n.b == Binop.Blt) result =
            ((Econst) n.e1).i < ((Econst) n.e2).i ? 1 : 0;
          if (n.b == Binop.Ble) result =
            ((Econst) n.e1).i <= ((Econst) n.e2).i ? 1 : 0;
        } else if (n.b == Binop.Band) {
          if (((Econst) n.e1).i == 0) result = 0; else {
            if (((Econst) n.e2).i == 0) result = 0; else result = 1;
          }
        } else if (n.b == Binop.Bor) {
          if (((Econst) n.e1).i != 0) result = 1; else {
            if (((Econst) n.e2).i != 0) result = 1; else result = 0;
          }
        }

        return new Econst(result);
      } else if (
        n.e1 instanceof Econst && n.b == Binop.Bdiv && ((Econst) n.e1).i == 0
      ) {
        if (
          reduction(n.e2) instanceof Econst && ((Econst) reduction(n.e2)).i == 0
        ) return nexpr;
      } else if (
        n.b == Binop.Bsub && n.e1.toString().equals(n.e2.toString())
      ) return new Econst(0); else if (
        n.e2 instanceof Econst && n.b == Binop.Bdiv && ((Econst) n.e2).i == 0
      ) return nexpr; else if (n.e2 instanceof Eunop) {
        if (n.b == Binop.Bsub) {
          if (((Eunop) n.e2).u == Unop.Uneg) {
            return reduction(new Ebinop(Binop.Badd, n.e1, ((Eunop) n.e2).e));
          } else if (((Eunop) n.e2).u == Unop.Unot) {
            Expr e = reduction(((Eunop) n.e2).e);
            return reduction(new Ebinop(Binop.Bsub, n.e1, e));
          }
        } else if (n.b == Binop.Badd) {
          if (((Eunop) n.e2).u == Unop.Uneg) {
            return reduction(new Ebinop(Binop.Bsub, n.e1, ((Eunop) n.e2).e));
          } else if (((Eunop) n.e2).u == Unop.Unot) {
            Expr e = reduction(((Eunop) n.e2));
            if (
              e.toString().equals(((Eunop) n.e2).toString())
            ) return new Ebinop(Binop.Badd, n.e1, e);
            return reduction(new Ebinop(Binop.Badd, n.e1, e));
          }
        }
      } else {
        Expr e = reduction(n.e1);
        Expr f = reduction(n.e2);

        if (
          e.toString().equals((n.e1).toString()) &&
          f.toString().equals((n.e2).toString())
        ) return new Ebinop(n.b, e, f);
        return reduction(new Ebinop(n.b, e, f));
      }
    } else if (nexpr instanceof Eunop) {
      Eunop n = (Eunop) nexpr;

      if (n.e instanceof Econst) {
        if (n.u == Unop.Uneg) return new Econst(-((Econst) n.e).i); else {
          if (((Econst) n.e).i == 0) return new Econst(
            1
          ); else return new Econst(0);
        }
      } else {
        Expr e = reduction(n.e);
        if (e instanceof Econst) return reduction(new Eunop(n.u, e)); else {
          return new Eunop(n.u, e);
        }
      }
    }

    return nexpr;
  }

  public void visit(Ebinop nbinop) {
    // if (debug) System.out.println("Ebinop Deb");
    Expr temp = reduction(nbinop);
    if (temp instanceof Econst) {
      Register rd = stack_reg.pop();
      Label Ld = stack_lab.pop();
      Label L1 = new Label();
      current_graph.graph.put(L1, new Rconst(((Econst) temp).i, rd, Ld));
      last_result = L1;
      return;
    } else if (temp instanceof Ebinop) {
      Ebinop n = (Ebinop) temp;
      Mbinop m = null;
      if (n.b == Binop.Badd) m = Mbinop.Madd;
      if (n.b == Binop.Bsub) m = Mbinop.Msub;
      if (n.b == Binop.Bmul) m = Mbinop.Mmul;
      if (n.b == Binop.Bdiv) m = Mbinop.Mdiv;
      if (n.b == Binop.Beq) m = Mbinop.Msete;
      if (n.b == Binop.Bneq) m = Mbinop.Msetne;
      if (n.b == Binop.Bge) m = Mbinop.Msetge;
      if (n.b == Binop.Bgt) m = Mbinop.Msetg;
      if (n.b == Binop.Ble) m = Mbinop.Msetle;
      if (n.b == Binop.Blt) m = Mbinop.Msetl;

      if (n.e1 instanceof Econst && n.e2 instanceof Eaccess_local) {
        Register rd = stack_reg.pop();
        Label Ld = stack_lab.pop();
        Label L3 = new Label();
        stack_lab.push(L3);
        Register r2 = new Register();

        Munop um = null;
        if (n.b != Binop.Band && n.b != Binop.Bor) {
          if (n.b == Binop.Badd) um = new Maddi(((Econst) n.e1).i); else if (
            n.b == Binop.Beq
          ) um = new Msetei(((Econst) n.e1).i); else if (n.b == Binop.Bneq) um =
            new Msetnei(((Econst) n.e1).i); else if (
            n.b == Binop.Bdiv || n.b == Binop.Bsub
          ) {
            r2 = var_locals.get(((Eaccess_local) n.e2).i);
            stack_reg.push(rd);
            this.visit(n.e1);
            Label L6 = new Label();
            Label L7 = new Label();
            Label L8 = new Label();
            Register r3 = new Register();
            Register r4 = new Register();
            current_graph.graph.put(L3, new Rmbinop(Mbinop.Mmov, r2, r3, L6));
            current_graph.graph.put(L6, new Rmbinop(Mbinop.Mmov, rd, r4, L7));
            current_graph.graph.put(L7, new Rmbinop(m, r3, r4, L8));
            current_graph.graph.put(L8, new Rmbinop(Mbinop.Mmov, r4, rd, Ld));
            reg_retour.push(rd);
            return;
          } else {
            r2 = var_locals.get(((Eaccess_local) n.e2).i);
            stack_reg.push(rd);
            this.visit(n.e1);
            Label L6 = new Label();
            Label L7 = new Label();
            Label L8 = new Label();
            Register r3 = new Register();
            Register r4 = new Register();
            current_graph.graph.put(L3, new Rmbinop(Mbinop.Mmov, r2, r3, L6));
            current_graph.graph.put(L6, new Rmbinop(Mbinop.Mmov, rd, r4, L7));

            current_graph.graph.put(L7, new Rmbinop(m, r4, r3, L8));
            current_graph.graph.put(L8, new Rmbinop(Mbinop.Mmov, r3, rd, Ld));
            reg_retour.push(rd);
            return;
          }
          stack_reg.push(rd);
          this.visit(n.e2);
          current_graph.graph.put(L3, new Rmunop(um, rd, Ld));
          reg_retour.push(rd);
          return;
        } else {
          if (n.b == Binop.Band) {
            Label L5 = new Label();
            if (((Econst) n.e1).i == 0) {
              current_graph.graph.put(L5, new Rconst(0, rd, Ld));
              last_result = L5;
              reg_retour.push(rd);
              return;
            }
            Label Lt, Lf;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            Lf = last_result;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Lt = last_result;
            RTLc(n.e2, Lt, Lf);
            return;
          } else if (n.b == Binop.Bor) {
            Label L5 = new Label();
            if (((Econst) n.e1).i != 0) {
              current_graph.graph.put(L5, new Rconst(1, rd, Ld));
              reg_retour.push(rd);
              last_result = L5;
              return;
            }
            Label Lt, Lf;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            Lf = last_result;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Lt = last_result;
            RTLc(n.e2, Lt, Lf);
            return;
          }
          return;
        }
      } else if (n.e2 instanceof Econst && n.e1 instanceof Eaccess_local) {
        Register rd = stack_reg.pop();
        Label Ld = stack_lab.pop();
        Label L3 = new Label();
        stack_lab.push(L3);
        Register r2 = new Register();

        Munop um = null;
        if (n.b != Binop.Band && n.b != Binop.Bor) {
          if (n.b == Binop.Badd) um = new Maddi(((Econst) n.e2).i); else if (
            n.b == Binop.Bsub
          ) um = new Maddi(-((Econst) n.e2).i); else if (n.b == Binop.Beq) um =
            new Msetei(((Econst) n.e2).i); else if (n.b == Binop.Bneq) um =
            new Msetnei(((Econst) n.e2).i); else {
            r2 = var_locals.get(((Eaccess_local) n.e1).i);
            stack_reg.push(rd);
            this.visit(n.e2);
            Label L6 = new Label();
            Label L7 = new Label();
            Label L8 = new Label();
            Register r3 = new Register();
            Register r4 = new Register();
            current_graph.graph.put(L3, new Rmbinop(Mbinop.Mmov, r2, r3, L6));
            current_graph.graph.put(L6, new Rmbinop(Mbinop.Mmov, rd, r4, L7));
            current_graph.graph.put(L7, new Rmbinop(m, r4, r3, L8));
            current_graph.graph.put(L8, new Rmbinop(Mbinop.Mmov, r3, rd, Ld));
            reg_retour.push(rd);
            return;
          }

          stack_reg.push(rd);
          this.visit(n.e1);
          current_graph.graph.put(L3, new Rmunop(um, rd, Ld));
          reg_retour.push(rd);
          return;
        } else {
          if (n.b == Binop.Band) {
            Label Lf;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            Lf = last_result;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Label L5 = new Label();
            RTLc(n.e1, L5, Lf);
            if (((Econst) n.e2).i == 0) current_graph.graph.put(
              L5,
              new Rconst(0, rd, Ld)
            ); else current_graph.graph.put(L5, new Rconst(1, rd, Ld));
            reg_retour.push(rd);
            return;
          } else if (n.b == Binop.Bor) {
            Label Lt;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Lt = last_result;
            Label L5 = new Label();
            RTLc(n.e1, Lt, L5);
            if (((Econst) n.e2).i != 0) current_graph.graph.put(
              L5,
              new Rconst(1, rd, Ld)
            ); else current_graph.graph.put(L5, new Rconst(0, rd, Ld));
            reg_retour.push(rd);
            return;
          }
          return;
        }
      } else if (
        n.e1 instanceof Eaccess_local && n.e2 instanceof Eaccess_local
      ) {
        Register r2 = var_locals.get(((Eaccess_local) n.e1).i);
        Register r5 = var_locals.get(((Eaccess_local) n.e2).i);
        if (n.b != Binop.Band && n.b != Binop.Bor) {
          Register rd = stack_reg.pop();
          Label Ld = stack_lab.pop();
          Label L3 = new Label();
          Label L6 = new Label();
          Label L7 = new Label();
          Label L8 = new Label();
          Register r3 = new Register();
          Register r4 = new Register();
          current_graph.graph.put(L3, new Rmbinop(Mbinop.Mmov, r2, r3, L6));
          current_graph.graph.put(L6, new Rmbinop(Mbinop.Mmov, r5, r4, L7));
          current_graph.graph.put(L7, new Rmbinop(m, r4, r3, L8));
          current_graph.graph.put(L8, new Rmbinop(Mbinop.Mmov, r3, rd, Ld));
          last_result = L3;
          reg_retour.push(rd);
          return;
        } else {
          if (n.b == Binop.Band) {
            Label Lt, Lf;
            Register rd = stack_reg.pop();
            Label Ld = stack_lab.pop();
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            Lf = last_result;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Lt = last_result;
            RTLc(n.e2, Lt, Lf);
            RTLc(n.e1, last_result, Lf);
            return;
          } else if (n.b == Binop.Bor) {
            Label Lt, Lf;
            Register rd = stack_reg.pop();
            Label Ld = stack_lab.pop();
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            Lf = last_result;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Lt = last_result;
            RTLc(n.e2, Lt, Lf);
            RTLc(n.e1, Lt, last_result);
            return;
          }
        }
      } else {
        if (n.b != Binop.Band && n.b != Binop.Bor) {
          Register rd = stack_reg.pop();
          Label Ld = stack_lab.pop();
          Label L3 = new Label();
          stack_lab.push(L3);
          Register r2 = new Register();
          stack_reg.push(r2);
          this.visit(n.e1);
          stack_reg.push(rd);
          this.visit(n.e2);
          Label L6 = new Label();
          Label L7 = new Label();
          Label L8 = new Label();
          Register r3 = new Register();
          Register r4 = new Register();
          current_graph.graph.put(L3, new Rmbinop(Mbinop.Mmov, r2, r3, L6));
          current_graph.graph.put(L6, new Rmbinop(Mbinop.Mmov, rd, r4, L7));
          current_graph.graph.put(L7, new Rmbinop(m, r4, r3, L8));
          current_graph.graph.put(L8, new Rmbinop(Mbinop.Mmov, r3, rd, Ld));
          reg_retour.push(rd);
          return;
        } else {
          if (n.b == Binop.Band) {
            Label Lt, Lf;
            Register rd = stack_reg.pop();
            Label Ld = stack_lab.pop();
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            Lf = last_result;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Lt = last_result;
            RTLc(n.e2, Lt, Lf);
            RTLc(n.e1, last_result, Lf);
            return;
          } else if (n.b == Binop.Bor) {
            Label Lt, Lf;
            Register rd = stack_reg.pop();
            Label Ld = stack_lab.pop();

            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(0));
            Lf = last_result;
            stack_reg.push(rd);
            stack_lab.push(Ld);
            this.visit(new Econst(1));
            Lt = last_result;
            RTLc(n.e2, Lt, Lf);
            RTLc(n.e1, Lt, last_result);
            return;
          }
        }
      }
    }
    // if (debug) System.out.println("Ebinop Fin");
  }

  public void visit(Eunop nexpr) {
    // if (debug) System.out.println("Eunop Deb");
    Expr temp = reduction(nexpr);
    if (temp instanceof Eunop) {
      Eunop n = (Eunop) temp;
      if (n.u == Unop.Uneg) {
        Ebinop e = new Ebinop(Binop.Bsub, new Econst(0), n);
        this.visit(e);
        return;
      } else if (n.u == Unop.Unot) {
        Register rd = stack_reg.pop();
        Label Ld = stack_lab.pop();

        Label Lt, Lf;

        stack_reg.push(rd);
        stack_lab.push(Ld);
        this.visit(new Econst(0));
        Lt = last_result;

        stack_reg.push(rd);
        stack_lab.push(Ld);
        this.visit(new Econst(1));
        Lf = last_result;

        RTLc(n.e, Lt, Lf);
        return;
      }
    } else {
      return;
    }
  }

  public void visit(Tint n) {
    // if (debug) System.out.println("Tint Deb");
    Register rd = stack_reg.pop();
    Label Ld = stack_lab.pop();
    Label L1 = new Label();
    current_graph.graph.put(L1, new Rconst(0, rd, Ld));

    last_result = L1;
    // if (debug) System.out.println("Tint Fin");
  }

  public void visit(Tstructp n) {
    // if (debug) System.out.println("Tstructp Deb");
    Register rd = stack_reg.pop();
    Label Ld = stack_lab.pop();
    Label L1 = new Label();

    current_graph.graph.put(L1, new Rconst(0, rd, Ld));
    last_result = L1;
    // if (debug) System.out.println("Tstructp Fin");
  }

  public void visit(Eaccess_local n) {
    // if (debug) System.out.println("Eaccess_local Deb");
    Register r = var_locals.get(n.i);
    Register rd = stack_reg.pop();
    Label Ld = stack_lab.pop();
    Label L1 = new Label();
    current_graph.graph.put(L1, new Rmbinop(Mbinop.Mmov, r, rd, Ld));
    last_result = L1;
    // if (debug) System.out.println("Eaccess_local Fin");
  }

  public void visit(Eaccess_field n) {
    // if (debug) System.out.println("Eaccess_field Deb");
    if (n.e instanceof Eaccess_local) {
      Register r = var_locals.get(((Eaccess_local) n.e).i);
      stack_var_locals.push(var_locals);
      int i = n.f.pos;
      Register rd = stack_reg.pop();
      Label Ld = stack_lab.pop();
      Label L1 = new Label();
      current_graph.graph.put(L1, new Rload(r, Memory.word_size * i, rd, Ld));
      last_result = L1;
    } else if (n.e instanceof Eaccess_field) {
      int i = n.f.pos;
      Register rd = stack_reg.pop();
      Label Ld = stack_lab.pop();
      Label L1 = new Label();
      Register rtemp = new Register();
      stack_lab.push(L1);
      stack_reg.push(rtemp);
      this.visit(n.e);
      current_graph.graph.put(
        L1,
        new Rload(rtemp, Memory.word_size * i, rd, Ld)
      );
    }
  }

  public void visit(Eassign_local n) {
    // if (debug) System.out.println("Eassign_local Deb");
    Register r = var_locals.get(n.i);
    Register rd = stack_reg.pop();
    Label Ld = stack_lab.pop();
    Label L2 = new Label();
    Label L1 = new Label();
    if (n.e instanceof Econst) {
      current_graph.graph.put(L1, new Rconst(((Econst) n.e).i, r, L2));
      current_graph.graph.put(L2, new Rconst(((Econst) n.e).i, rd, Ld));
      reg_retour.push(r);
      last_result = L1;
      return;
    }
    if (n.e instanceof Eaccess_local) {
      Register r2 = var_locals.get(((Eaccess_local) n.e).i);
      current_graph.graph.put(L1, new Rmbinop(Mbinop.Mmov, r2, r, L2));
      current_graph.graph.put(L2, new Rmbinop(Mbinop.Mmov, r2, rd, Ld));
      last_result = L1;
      reg_retour.push(r);
      return;
    }
    if (n.e instanceof Esizeof) {
      current_graph.graph.put(
        L1,
        new Rconst(((Esizeof) n.e).s.fields.size(), rd, Ld)
      );
      last_result = L1;
      return;
    }
    Register r2 = new Register();
    stack_lab.push(L1);
    stack_reg.push(r2);
    this.visit(n.e);
    current_graph.graph.put(L1, new Rmbinop(Mbinop.Mmov, r2, r, L2));
    current_graph.graph.put(L2, new Rmbinop(Mbinop.Mmov, r2, rd, Ld));
    // if (debug) System.out.println("(Eassign_local Fin");
  }

  public void visit(Eassign_field n) {
    // if (debug) System.out.println("Eassign field deb");
    Eaccess_field tmp = (Eaccess_field) n.e1;
    int i;
    if (tmp.e instanceof Eaccess_local) {
      Register r = var_locals.get(((Eaccess_local) tmp.e).i);
      i = n.f.pos;
      Register rd = stack_reg.pop();
      Label Ld = stack_lab.pop();
      Label L1 = new Label();
      Register rr = new Register();
      stack_reg.push(rr);
      stack_lab.push(L1);
      this.visit(n.e2);
      Label L2 = new Label();
      current_graph.graph.put(L1, new Rstore(rr, r, Memory.word_size * i, L2));
      current_graph.graph.put(L2, new Rmbinop(Mbinop.Mmov, rr, rd, Ld));
    } else if (tmp.e instanceof Eaccess_field) {
      Eaccess_field tmpe = (Eaccess_field) tmp.e;
      i = n.f.pos;
      Register rd = stack_reg.pop();
      Label Ld = stack_lab.pop();
      Label L1 = new Label();
      Register rtemp = new Register();
      stack_lab.push(L1);
      stack_reg.push(rtemp);
      this.visit(tmpe);
      Label L2 = last_result;
      Register rtemp2 = new Register();
      stack_lab.push(L2);
      stack_reg.push(rtemp2);
      this.visit(n.e2);
      Label L4 = new Label();
      current_graph.graph.put(
        L1,
        new Rstore(rtemp2, rtemp, Memory.word_size * i, L4)
      );
      current_graph.graph.put(L4, new Rmbinop(Mbinop.Mmov, rtemp2, rd, Ld));
    }
    // if (debug) System.out.println("Eassign field fin");
  }

  public void visit(Ecall n) {
    // if (debug) System.out.println("Ecall Deb");
    List<Register> rl = new LinkedList<>();

    //reverse direction
    int ss = n.el.size();
    int i = 0;
    Register rd = stack_reg.pop();
    Label Ld = stack_lab.pop();
    Label L1, L2;
    if (ss == 0) {
      L1 = new Label();
      current_graph.graph.put(L1, new Rcall(rd, n.i, rl, Ld));
      last_result = L1;
      return;
    }
    Label L3 = new Label();
    Register r2 = new Register();
    stack_lab.push(L3);
    stack_reg.push(r2);
    rl.add(r2);
    for (Expr e : n.el) {
      Expr ee = reduction(e);
      this.visit(ee);
      L2 = last_result;
      stack_lab.push(L2);
      if (i++ == ss - 1) stack_reg.push(rd); else {
        Register rr = new Register();
        stack_reg.push(rr);
        rl.add(rr);
      }
      last_result = L2;
    }
    current_graph.graph.put(L3, new Rcall(rd, n.i, rl, Ld));
    // if (debug) System.out.println("Ecall Fin");
  }

  public void visit(Esizeof n) {
    Register rd = stack_reg.pop();
    Label Ld = stack_lab.pop();
    Label L1 = new Label();
    current_graph.graph.put(
      L1,
      new Rconst(Memory.word_size * n.s.fields.size(), rd, Ld)
    );
    last_result = L1;
  }

  public void visit(Sskip n) {
    // if (debug) System.out.println("Deb Sskip");
    last_result = stack_lab.pop();
    // if (debug) System.out.println("Fin Sskip");
  }

  public void visit(Sexpr n) {
    // if (debug) System.out.println("Sexpr Deb");
    this.visit(n.e);
    // if (debug) System.out.println("Sexpr Fin");
  }

  public Label RTLc(Expr e, Label Lt, Label Lf) {
    // if (debug) System.out.println("RTLC deb");
    Register r = new Register();
    Label L2 = new Label();
    stack_reg.push(r);
    stack_lab.push(L2);

    if (e instanceof Ebinop) {
      Ebinop tmp = (Ebinop) e;
      if (tmp.b == Binop.Band) {
        RTLc(tmp.e2, Lt, Lf);
        return RTLc(tmp.e1, last_result, Lf);
      }

      if (tmp.b == Binop.Bor) {
        RTLc(tmp.e2, Lt, Lf);
        return RTLc(tmp.e1, Lt, last_result);
      }

      if (tmp.b == Binop.Ble) {
        Label LL3 = new Label();
        Label LL2;
        Register rr2 = new Register();
        Register rr1 = new Register();
        stack_lab.push(LL3);
        stack_reg.push(rr2);
        this.visit(tmp.e2);
        LL2 = last_result;
        stack_lab.push(LL2);
        stack_reg.push(rr1);
        this.visit(tmp.e1);
        Mbbranch m;
        if (tmp.b == Binop.Ble) m = Mbbranch.Mjle; else m = Mbbranch.Mjl;
        current_graph.graph.put(LL3, new Rmbbranch(m, rr2, rr1, Lt, Lf));
        return last_result;
      }

      if (tmp.e2 instanceof Econst) {
        if (tmp.b == Binop.Ble) {
          Register rrd = stack_reg.pop();
          Label LL3 = new Label();
          Register rr2 = new Register();
          stack_lab.push(LL3);
          stack_reg.push(rr2);
          Mubranch um;
          stack_reg.push(rrd);
          this.visit(tmp.e1);
          L2 = last_result;
          um = new Mjlei(((Econst) tmp.e2).i);
          current_graph.graph.put(LL3, new Rmubranch(um, rrd, Lt, Lf));
          return last_result;
        }
        if (tmp.b == Binop.Bgt) {
          Register rrd = stack_reg.pop();
          Label LL3 = new Label();
          Register rr2 = new Register();
          stack_lab.push(LL3);
          stack_reg.push(rr2);
          Mubranch um;
          stack_reg.push(rrd);
          this.visit(tmp.e1);
          L2 = last_result;
          um = new Mjgi(((Econst) tmp.e2).i);
          current_graph.graph.put(LL3, new Rmubranch(um, rrd, Lt, Lf));
          return last_result;
        }
      }
    }
    this.visit(e);
    Mjz m = new Mjz();
    current_graph.graph.put(L2, new Rmubranch(m, r, Lf, Lt));
    // if (debug) System.out.println(
    //   "RTLC fin " + L2 + new Rmubranch(m, r, Lf, Lt)
    // );
    return L2;
  }

  public void visit(Sif n) {
    // if (debug) System.out.println("Deb Sif");
    Label Lt, Lf;
    Stack<Label> stack_lab_tmp = (Stack<Label>) stack_lab.clone();
    Stack<Register> stack_reg_tmp = (Stack<Register>) stack_reg.clone();
    this.visit(n.s2);
    Lf = last_result;
    stack_lab = stack_lab_tmp;
    stack_reg = stack_reg_tmp;
    this.visit(n.s1);
    Lt = last_result;
    RTLc(n.e, Lt, Lf);
  }

  public void visit(Swhile n) {
    // if (debug) System.out.println("Deb Swhile");
    Label Ld = stack_lab.pop();
    Label L = new Label();
    stack_lab.push(L);
    this.visit(n.s);
    Label l = last_result;
    RTLc(n.e, l, Ld);
    current_graph.graph.put(L, new Rgoto(last_result));
    // if (debug) System.out.println("Fin Swhile");
  }
}
