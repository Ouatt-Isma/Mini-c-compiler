package mini_c;

import java.util.Stack;

class ToLTL extends EmptyERTLERTLVisitor {

  private LTLfile ltlfile;

  private Coloring coloring; // coloriage de la fonction en cours de traduction
  int size_locals; // taille pour les variables locales
  LTLgraph graph; // graphe en cours de construction fun;

  Stack<Label> stack_label = new Stack<>(); // Pour avoir le label de l'instruction exécutée
  public boolean debug = false;

  LTLfile translate(ERTLfile ertl) {
    this.visit(ertl);
    return ltlfile;
  }

  public void visit(ERTLfile ertl) {
    // if (debug) System.out.println("ERTLfile Deb");
    ltlfile = new LTLfile();
    for (ERTLfun d : ertl.funs) {
      this.visit(d);
    }
    // if (debug) System.out.println("ERTLfile Fin");
  }

  public void visit(ERTLfun d) {
    // if (debug) System.out.println("ERTLfun Deb" + d.name);
    Liveness _Liveness = new Liveness(d.body);
    Interference _interference = new Interference(_Liveness);
    // if (debug) _interference.print();
    coloring = new Coloring(_interference);
    // if (debug) 
    //coloring.print();
    size_locals = coloring.nlocals;
    LTLfun tmp = new LTLfun(d.name);

    tmp.entry = d.entry;
    tmp.body = new LTLgraph();
    graph = tmp.body;
    for (Label L : d.body.graph.keySet()) {
      stack_label.push(L);
      ERTL ertl = d.body.graph.get(L);
      this.visit(ertl);
    }
    ltlfile.funs.add(tmp);
    // if (debug) System.out.println("ERTLfun Fin" + d.name);
  }

  public void visit(ERTL ertl) {
    if (ertl instanceof ERconst) {
      this.visit((ERconst) ertl);
    } else if (ertl instanceof ERload) {
      this.visit((ERload) ertl);
    } else if (ertl instanceof ERstore) {
      this.visit((ERstore) ertl);
    } else if (ertl instanceof ERmunop) {
      this.visit((ERmunop) ertl);
    } else if (ertl instanceof ERmbinop) {
      this.visit((ERmbinop) ertl);
    } else if (ertl instanceof ERmubranch) {
      this.visit((ERmubranch) ertl);
    } else if (ertl instanceof ERmbbranch) {
      this.visit((ERmbbranch) ertl);
    } else if (ertl instanceof ERgoto) {
      this.visit((ERgoto) ertl);
    } else if (ertl instanceof ERcall) {
      this.visit((ERcall) ertl);
    } else if (ertl instanceof ERalloc_frame) {
      this.visit((ERalloc_frame) ertl);
    } else if (ertl instanceof ERdelete_frame) {
      this.visit((ERdelete_frame) ertl);
    } else if (ertl instanceof ERreturn) {
      this.visit((ERreturn) ertl);
    } else if (ertl instanceof ERcall) {
      this.visit((ERcall) ertl);
    } else if (ertl instanceof ERgoto) {
      this.visit((ERgoto) ertl);
    } else if (ertl instanceof ERpush_param) {
      this.visit((ERpush_param) ertl);
    } else if (ertl instanceof ERget_param) {
      this.visit((ERget_param) ertl);
    }
  }

  public void visit(ERconst ertl) {
    // if (debug) System.out.println("ERconst Deb");
    Label L = stack_label.pop();
    Operand c = coloring.colors.get(ertl.r);
    if (c == null) c = new Reg(ertl.r);
    graph.put(L, new Lconst(ertl.i, c, ertl.l));
    // if (debug) System.out.println("ERconst Fin");
  }

  public void visit(ERload ertl) {
    // if (debug) System.out.println("ERload Deb");

    Label L = stack_label.pop();
    if (
      coloring.colors.get(ertl.r1) instanceof Reg &&
      coloring.colors.get(ertl.r2) instanceof Reg
    ) {
      graph.put(
        L,
        new Lload(
          ((Reg) coloring.colors.get(ertl.r1)).r,
          ertl.i,
          ((Reg) coloring.colors.get(ertl.r2)).r,
          ertl.l
        )
      );

      return;
    } else if (
      coloring.colors.get(ertl.r1) instanceof Reg &&
      coloring.colors.get(ertl.r2) instanceof Spilled
    ) {
      Spilled s2 = ((Spilled) coloring.colors.get(ertl.r2));
      Label tmp_e = L;
      Label tmp_s = new Label();

      graph.put(
        tmp_e,
        new Lload(
          ((Reg) coloring.colors.get(ertl.r1)).r,
          ertl.i,
          Register.tmp1,
          tmp_s
        )
      );

      tmp_e = tmp_s;
      tmp_s = ertl.l;

      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, s2, new Reg(Register.tmp1), tmp_s)
      );

      return;
    } else if (
      coloring.colors.get(ertl.r1) instanceof Spilled &&
      coloring.colors.get(ertl.r2) instanceof Reg
    ) {
      Spilled s1 = ((Spilled) coloring.colors.get(ertl.r1));
      Label tmp_e = L;
      Label tmp_s = new Label();
      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, s1, new Reg(Register.tmp1), tmp_s)
      );
      tmp_e = tmp_s;
      tmp_s = ertl.l;
      graph.put(
        tmp_e,
        new Lload(
          Register.tmp1,
          ertl.i,
          ((Reg) coloring.colors.get(ertl.r2)).r,
          tmp_s
        )
      );
      //throw new Error("");
      return;
    } else {
      Spilled s1 = ((Spilled) coloring.colors.get(ertl.r1));
      Spilled s2 = ((Spilled) coloring.colors.get(ertl.r2));
      Label tmp_e = L;
      Label tmp_s = new Label();
      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, s1, new Reg(Register.tmp1), tmp_s)
      );

      tmp_e = tmp_s;
      tmp_s = new Label();

      graph.put(tmp_e, new Lload(Register.tmp1, ertl.i, Register.tmp2, tmp_s));

      tmp_e = tmp_s;
      tmp_s = ertl.l;
      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, new Reg(Register.tmp2), s2, tmp_s)
      );
    }
    // if (debug) System.out.println("ERload Fin");
  }

  public void visit(ERstore ertl) {
    // if (debug) System.out.println("ERstore Deb");
    Label L = stack_label.pop();
    if (
      coloring.colors.get(ertl.r1) instanceof Reg &&
      coloring.colors.get(ertl.r2) instanceof Reg
    ) {
      graph.put(
        L,
        new Lstore(
          ((Reg) coloring.colors.get(ertl.r1)).r,
          ((Reg) coloring.colors.get(ertl.r2)).r,
          ertl.i,
          ertl.l
        )
      );
      return;
    }
    if (
      coloring.colors.get(ertl.r1) instanceof Reg &&
      coloring.colors.get(ertl.r2) instanceof Spilled
    ) {
      Spilled s2 = ((Spilled) coloring.colors.get(ertl.r2));
      Label tmp_e = L;
      Label tmp_s = new Label();
      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, s2, new Reg(Register.tmp1), tmp_s)
      );
      tmp_e = tmp_s;
      tmp_s = ertl.l;
      graph.put(
        tmp_e,
        new Lstore(
          ((Reg) coloring.colors.get(ertl.r1)).r,
          Register.tmp1,
          ertl.i,
          tmp_s
        )
      );
      //throw new Error("");
      return;
    }

    if (
      coloring.colors.get(ertl.r1) instanceof Spilled &&
      coloring.colors.get(ertl.r2) instanceof Reg
    ) {
      Spilled s1 = ((Spilled) coloring.colors.get(ertl.r1));
      Label tmp_e = L;
      Label tmp_s = new Label();
      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, s1, new Reg(Register.tmp1), tmp_s)
      );
      tmp_e = tmp_s;
      tmp_s = ertl.l;
      graph.put(
        tmp_e,
        new Lstore(
          Register.tmp1,
          ((Reg) coloring.colors.get(ertl.r1)).r,
          ertl.i,
          tmp_s
        )
      );
      //throw new Error("");
      return;
    }

    if (
      coloring.colors.get(ertl.r1) instanceof Spilled &&
      coloring.colors.get(ertl.r2) instanceof Spilled
    ) {
      Spilled s1 = ((Spilled) coloring.colors.get(ertl.r1));
      Spilled s2 = ((Spilled) coloring.colors.get(ertl.r1));
      Label tmp_e = L;
      Label tmp_s = new Label();
      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, s1, new Reg(Register.tmp1), tmp_s)
      );
      tmp_e = tmp_s;
      tmp_s = new Label();
      graph.put(
        tmp_e,
        new Lmbinop(Mbinop.Mmov, s2, new Reg(Register.tmp2), tmp_s)
      );

      tmp_e = tmp_s;
      tmp_s = ertl.l;
      graph.put(tmp_e, new Lstore(Register.tmp1, Register.tmp2, ertl.i, tmp_s));
    }
    // if (debug) System.out.println("ERstore Fin");

  }

  public void visit(ERmunop ertl) {
    // if (debug) System.out.println("ERmunop Deb");
    Label L = stack_label.pop();
    Operand c = coloring.colors.get(ertl.r);
    if (c == null) c = new Reg(ertl.r);
    graph.put(L, new Lmunop(ertl.m, c, ertl.l));
    // if (debug) System.out.println("ERmunop Fin");
  }

  public void visit(ERmbinop ertl) {
    // if (debug) System.out.println("ERmbinop Deb");
    Label L = stack_label.pop();
    Operand r1 = coloring.colors.get(ertl.r1);
    if (r1 == null) r1 = new Reg(ertl.r1);
    Operand r2 = coloring.colors.get(ertl.r2);
    if (r2 == null) r2 = new Reg(ertl.r2);

    if (ertl.m == Mbinop.Mmov && r1.equals(r2)) graph.put(
      L,
      new Lgoto(ertl.l)
    ); else if (ertl.m == Mbinop.Mmul && r2 instanceof Spilled) {
      Reg reg = new Reg(Register.tmp2);
      Label temp = new Label(), temp2 = new Label();
      graph.put(L, new Lmbinop(Mbinop.Mmov, r2, reg, temp));
      graph.put(temp, new Lmbinop(ertl.m, r1, reg, temp2));
      graph.put(temp2, new Lmbinop(Mbinop.Mmov, reg, r2, ertl.l));
    } else if (r1 instanceof Spilled && r2 instanceof Spilled) {
      Reg reg = new Reg(Register.tmp2);
      Label temp = new Label(), temp2 = new Label();
      graph.put(L, new Lmbinop(Mbinop.Mmov, r2, reg, temp));
      graph.put(temp, new Lmbinop(ertl.m, r1, reg, temp2));
      graph.put(temp2, new Lmbinop(Mbinop.Mmov, reg, r2, ertl.l));
    } else graph.put(L, new Lmbinop(ertl.m, r1, r2, ertl.l));
    // if (debug) System.out.println("ERmbinop Fin");
  }

  public void visit(ERalloc_frame ertl) {
    if (debug) System.out.println("ERalloc_frame Deb");
    Label L = stack_label.pop();
    int m = coloring.nlocals;
    if (m == 0) {
      Label tmp_s = new Label();
      Label tmp_e = L;
      graph.put(tmp_e, new Lpush(new Reg(Register.rbp), tmp_s));
      tmp_e = tmp_s;
      tmp_s = ertl.l;
      graph.put(
        tmp_e,
        new Lmbinop(
          Mbinop.Mmov,
          new Reg(Register.rsp),
          new Reg(Register.rbp),
          tmp_s
        )
      );

      return;
    }

    Label tmp_s = new Label();
    Label tmp_e = L;
    graph.put(tmp_e, new Lpush(new Reg(Register.rbp), tmp_s));
    tmp_e = tmp_s;
    tmp_s = new Label();
    graph.put(
      tmp_e,
      new Lmbinop(
        Mbinop.Mmov,
        new Reg(Register.rsp),
        new Reg(Register.rbp),
        tmp_s
      )
    );
    tmp_e = tmp_s;
    tmp_s = ertl.l;
    //System.out.println(m);
    graph.put(
      tmp_e,
      new Lmunop(new Maddi(-8 * (m)), new Reg(Register.rsp), tmp_s)
    );

    if (debug) System.out.println("ERalloc_frame Fin");
  }

  public void visit(ERdelete_frame ertl) {
    if (debug) System.out.println("ERdelete_frame Deb");
    Label L = stack_label.pop();
    int m = coloring.nlocals;
    if (m == 0) {
      graph.put(L, new Lpop(Register.rbp, ertl.l));
      return;
    }
    Label tmp_s = new Label();
    Label tmp_e = L;
    graph.put(
      tmp_e,
      new Lmbinop(
        Mbinop.Mmov,
        new Reg(Register.rbp),
        new Reg(Register.rsp),
        tmp_s
      )
    );

    tmp_e = tmp_s;
    tmp_s = ertl.l;
    graph.put(tmp_e, new Lpop(Register.rbp, tmp_s));
    if (debug) System.out.println("delete_frame Fin");
  }

  public void visit(ERreturn ertl) {
    if (debug) System.out.println("ERreturn Deb");
    Label L = stack_label.pop();
    graph.put(L, new Lreturn());
    if (debug) System.out.println("ERreturn Fin");
  }

  public void visit(ERcall ertl) {
    if (debug) System.out.println("ERcall Deb");
    Label L = stack_label.pop();
    graph.put(L, new Lcall(ertl.s, ertl.l));
    if (debug) System.out.println("ERcall Fin");
  }

  public void visit(ERmubranch ertl) {
    if (debug) System.out.println("ERmubranch Deb");
    Label L = stack_label.pop();
    Operand c = coloring.colors.get(ertl.r);
    if (c == null) {
      graph.put(L, new Lmubranch(ertl.m, c, ertl.l1, ertl.l2));
      return;
    }
    graph.put(L, new Lmubranch(ertl.m, c, ertl.l1, ertl.l2));
    if (debug) System.out.println("ERmubranch Fin");
  }

  public void visit(ERmbbranch ertl) {
    if (debug) System.out.println("ERmbbranch Deb");
    Label L = stack_label.pop();
    graph.put(
      L,
      new Lmbbranch(
        ertl.m,
        coloring.colors.get(ertl.r1),
        coloring.colors.get(ertl.r2),
        ertl.l1,
        ertl.l2
      )
    );
    //graph.put(L, new Lcall(ertl.s, ertl.l));

    if (debug) System.out.println("ERmbbranch Fin");
  }

  public void visit(ERgoto ertl) {
    if (debug) System.out.println("ERmbbranch Deb");
    Label L = stack_label.pop();
    graph.put(L, new Lgoto(ertl.l));
    //graph.put(L, new Lcall(ertl.s, ertl.l));

    if (debug) System.out.println("ERmbbranch Fin");
  }

  public void visit(ERpush_param ertl) {
    if (debug) System.out.println("ERpush_param Deb");
    Label L = stack_label.pop();
    Operand c = coloring.colors.get(ertl.r);
    if (c == null) {
      graph.put(L, new Lpush(new Reg(ertl.r), ertl.l));
      return;
    }

    graph.put(L, new Lpush(c, ertl.l));
    return;
    //graph.put(L, new Lcall(ertl.s, ertl.l));

    //if (debug) System.out.println("ERpush_param Fin");
  }

  public void visit(ERget_param ertl) {
    if (debug) System.out.println("ERpush_param Deb");
    Label L = stack_label.pop();
    Operand c = coloring.colors.get(ertl.r);

    if (c == null) {
      Label tmp_e = L;
      Label tmp_s = ertl.l;
      graph.put(tmp_e, new Lload(Register.rbp, ertl.i, Register.tmp1, tmp_s));


      return;
    }

    if (c instanceof Reg) {
      Reg tmp = (Reg) c;

      Label tmp_e = L;
      Label tmp_s = ertl.l;
      graph.put(tmp_e, new Lload(Register.rbp, ertl.i, tmp.r, tmp_s));
      return;
    }

    Spilled tmp = (Spilled) c;
    Label tmp_e = L;
    Label tmp_s = new Label();
    graph.put(tmp_e, new Lload(Register.rbp, ertl.i, Register.tmp1, tmp_s));
    tmp_e = tmp_s;
    tmp_s = ertl.l;
    graph.put(
      tmp_e,
      new Lmbinop(Mbinop.Mmov, new Reg(Register.tmp1), tmp, tmp_s)
    );

    if (debug) System.out.println("ERpush_param Fin");
  }
}
