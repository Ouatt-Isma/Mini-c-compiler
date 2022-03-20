package mini_c;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

class ToERTL extends EmptyRTLVisitor {

  private ERTLfile ertlfile; //The result after visiting the rtlfile
  // public boolean debug = false;
  ERTLgraph current_graph; //The graph of the function that we are visiting
  Stack<Label> stack_labelrtl = new Stack<>(); //To know the label of the instruction we are visiting
  ERTLfun current_fun; //current function we are visiting
  Set<Label> potential_rec;
  int n_reg_params = Register.parameters.size(); // number of registers to save parameters for a function called

  ERTLfile translate(RTLfile rtl) {
    this.visit(rtl);
    return ertlfile;
  }

  public void visit(RTLfile rtl) {
    // if (debug) System.out.println("RTL Deb");
    ertlfile = new ERTLfile();
    for (RTLfun d : rtl.funs) {
      this.visit(d);
    }
    // if (debug) System.out.println("RTL fin");
  }

  public void visit(RTLfun d) {
    // if (debug) System.out.println("RTLfun Deb" + d.name);
    ERTLfun tmp = new ERTLfun(d.name, d.formals.size());
    potential_rec = new HashSet<>();
    for (Register r : d.locals) {
      tmp.locals.add(r);
    }
    tmp.body = new ERTLgraph();
    current_graph = tmp.body;

    Label tmp_e, tmp_s;
    tmp_e = new Label();
    tmp_s = new Label();
    tmp.entry = tmp_e;

    // Allocate frame
    current_fun = tmp;
    current_graph.put(tmp_e, new ERalloc_frame(tmp_s));
    int i;

    // Save callee saved registers into temporary registers
    List<Register> CALLEE_SAVED = new LinkedList<>();
    Register tmp_r;
    int n = d.formals.size();
    int n_cs = Register.callee_saved.size();
    for (i = 0; i < n_cs; i++) {
      tmp_r = new Register();
      CALLEE_SAVED.add(tmp_r);
      tmp_e = tmp_s;
      if (i == (n_cs - 1) && n == 0) tmp_s = d.entry; else tmp_s = new Label();
      current_graph.put(
        tmp_e,
        new ERmbinop(Mbinop.Mmov, Register.callee_saved.get(i), tmp_r, tmp_s)
      );
    }
    Label ent = tmp_s; //entry when recursive terminal call

    //get value of parameters in the call
    int taille = (n > n_reg_params) ? n_reg_params : n;
    for (i = 0; i < taille; i++) {
      tmp_e = tmp_s;
      if (i == n - 1) tmp_s = d.entry; else tmp_s = new Label();
      current_graph.put(
        tmp_e,
        new ERmbinop(
          Mbinop.Mmov,
          Register.parameters.get(i),
          d.formals.get(i),
          tmp_s
        )
      );
    }

    //get the other values on the stack if more than n_reg_params
    for (i = n_reg_params; i < n; i++) {
      tmp_e = tmp_s;
      if (i == n - 1) tmp_s = d.entry; else tmp_s = new Label();
      current_graph.put(
        tmp_e,
        new ERget_param(
          ((n - 5) - (i - n_reg_params)) * Memory.word_size,
          d.formals.get(i),
          tmp_s
        )
      );
    }

    this.visit(d.body);

    // Appel récursif terminal ??

    for (Label l : potential_rec) {
      ERcall ercall = (ERcall) current_graph.graph.get(l);
      if (ercall.s.equals(d.name)) {
        ERmbinop movrax = (ERmbinop) current_graph.graph.get(ercall.l);
        if (movrax.l.equals(d.exit)) {
          current_graph.put(l, new ERgoto(ent));
        }
      }
    }

    tmp_e = d.exit;
    tmp_s = new Label();
    current_graph.put(
      tmp_e,
      new ERmbinop(Mbinop.Mmov, d.result, Register.result, tmp_s)
    );

    //Restore callee saved registers
    for (i = 0; i < Register.callee_saved.size(); i++) {
      tmp_r = CALLEE_SAVED.get(i);
      tmp_e = tmp_s;
      tmp_s = new Label();
      current_graph.put(
        tmp_e,
        new ERmbinop(Mbinop.Mmov, tmp_r, Register.callee_saved.get(i), tmp_s)
      );
    }
    tmp_e = tmp_s;
    tmp_s = new Label();
    current_graph.put(tmp_e, new ERdelete_frame(tmp_s));
    current_graph.put(tmp_s, new ERreturn());
    ertlfile.funs.add(tmp);
    // if (debug) System.out.println("RTLfun Fin");
  }

  public void visit(RTLgraph body) {
    // if (debug) System.out.println("RTLgraph Deb");
    for (Label L : body.graph.keySet()) {
      RTL rtl = body.graph.get(L);
      stack_labelrtl.push(L);
      this.visit(rtl);
    }

    // if (debug) System.out.println("RTLgraph Fin");
  }

  public void visit(RTL rtl) {
    if (rtl instanceof Rconst) {
      this.visit((Rconst) rtl);
    } else if (rtl instanceof Rload) {
      this.visit((Rload) rtl);
    } else if (rtl instanceof Rstore) {
      this.visit((Rstore) rtl);
    } else if (rtl instanceof Rmunop) {
      this.visit((Rmunop) rtl);
    } else if (rtl instanceof Rmbinop) {
      this.visit((Rmbinop) rtl);
    } else if (rtl instanceof Rmubranch) {
      this.visit((Rmubranch) rtl);
    } else if (rtl instanceof Rmbbranch) {
      this.visit((Rmbbranch) rtl);
    } else if (rtl instanceof Rgoto) {
      this.visit((Rgoto) rtl);
    } else if (rtl instanceof Rcall) {
      this.visit((Rcall) rtl);
    }
  }

  public void visit(Rconst rtl) {
    // if (debug) System.out.println("Rconst Deb");
    Label L = stack_labelrtl.pop();
    current_graph.put(L, new ERconst(rtl.i, rtl.r, rtl.l));
    // if (debug) System.out.println("Rconst Fin");
  }

  public void visit(Rload rtl) {
    // if (debug) System.out.println("Rload Deb");
    Label L = stack_labelrtl.pop();
    current_graph.put(L, new ERload(rtl.r1, rtl.i, rtl.r2, rtl.l));
    // if (debug) System.out.println("Rload Fin");
  }

  public void visit(Rstore rtl) {
    // if (debug) System.out.println("Rstore Deb");
    Label L = stack_labelrtl.pop();
    current_graph.put(L, new ERstore(rtl.r1, rtl.r2, rtl.i, rtl.l));
    // if (debug) System.out.println("Rstore Fin");
  }

  public void visit(Rmunop rtl) {
    // if (debug) System.out.println("Rmunop Deb");
    Label L = stack_labelrtl.pop();
    current_graph.put(L, new ERmunop(rtl.m, rtl.r, rtl.l));
    // if (debug) System.out.println("Rmunop Fin");
  }

  public void visit(Rmubranch rtl) {
    // if (debug) System.out.println("Rmubranch Deb");
    Label L = stack_labelrtl.pop();
    current_graph.put(L, new ERmubranch(rtl.m, rtl.r, rtl.l1, rtl.l2));
    // if (debug) System.out.println("Rmubranch Fin");
  }

  public void visit(Rmbbranch rtl) {
    // if (debug) System.out.println("Rmbbranch Deb");
    Label L = stack_labelrtl.pop();
    current_graph.put(L, new ERmbbranch(rtl.m, rtl.r1, rtl.r2, rtl.l1, rtl.l2));
    // if (debug) System.out.println("Rmbbranch Fin");
  }

  public void visit(Rgoto rtl) {
    // if (debug) System.out.println("Rgoto Deb");
    Label L = stack_labelrtl.pop();
    current_graph.put(L, new ERgoto(rtl.l));
    // if (debug) System.out.println("Rgoto Fin");
  }

  public void visit(Rmbinop rtl) {
    // if (debug) System.out.println("Rmbinop Deb");
    Label L = stack_labelrtl.pop();
    if (rtl.m == Mbinop.Mdiv) {
      Label L2 = new Label();
      Label L3 = new Label();
      current_graph.put(L, new ERmbinop(Mbinop.Mmov, rtl.r2, Register.rax, L2));
      current_graph.put(L2, new ERmbinop(rtl.m, rtl.r1, Register.rax, L3));
      current_graph.put(
        L3,
        new ERmbinop(Mbinop.Mmov, Register.rax, rtl.r2, rtl.l)
      );
    } else current_graph.put(L, new ERmbinop(rtl.m, rtl.r1, rtl.r2, rtl.l));

    // if (debug) System.out.println("Rmbinop Fin");
  }

  public void visit(Rcall rtl) {
    // if (debug) System.out.println("Rcall Deb");

    Label L = stack_labelrtl.pop();


    int n = rtl.rl.size();
    int taille = (n > n_reg_params) ? n_reg_params : n;
    Label temp;

    //Mov parameters of the function into registers for that (n_reg_params)

    for (int i = 0; i < taille; i++) {
      temp = new Label();
      current_graph.put(
        L,
        new ERmbinop(
          Mbinop.Mmov,
          rtl.rl.get(i),
          Register.parameters.get(i),
          temp
        )
      );
      L = temp;
    }

    //if the number of paramaters is more than n_reg_params, put the over on the stack
    
    for (int i = n_reg_params; i < n; i++) {
      temp = new Label();
      current_graph.put(L, new ERpush_param(rtl.rl.get(i), temp));
      L = temp;
    }
    temp = new Label();

    if (rtl.s.equals(current_fun.name)) {
      potential_rec.add(L);
    }
    current_graph.put(L, new ERcall(rtl.s, n, temp));
    L = temp;
    if (n <= n_reg_params) temp = rtl.l; else temp = new Label();

    current_graph.put(
      L,
      new ERmbinop(Mbinop.Mmov, Register.result, rtl.r, temp)
    );

    if (n > n_reg_params) {
      current_graph.put(
        temp,
        new ERmunop(new Maddi((n - n_reg_params) * Memory.word_size), Register.rsp, rtl.l)
      );
    }

    // if (debug) System.out.println("Rcall Fin");
  }
}
