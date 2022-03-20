package mini_c;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class Typing implements Pvisitor {

	// le résultat du typage sera mis dans cette variable
	
	private File file;
  	
	int nb_args;
		// nombre d'arguments d'une fonction
  	
  	int cursor = 0;			
  		// cette variable permet de distinguer les arguments
  		// d'une fonction de ses variables locales

	static final int IDENT_DECL = 0, IDENT_SRETURN = 2, IDENT_SIF =
	    3, IDENT_PEVAL = 4, IDENT_SWHILE = 5;
	int ident_block = 0;
	
	static final int IDENT_EUNOP = 5, IDENT_EBINOP = 6, IDENT_ECALL =
	    7, IDENT_PASSIGN = 10;
	
	static final int STRUCT_NAME = 1, STRUCT_VARNAME = 2, STRUCT_FIELD = 3;
	
	String NAME_FIELD;
	Typ TYP_FIELD;
	String ORIGIN_TYP;
	
	int nb_pblocs = 0;
		// permet de savoir la numéro du bloc sur lequel on est...
		// si np_blocs == 1, alors on est dans le bloc principal de la fonction 
	
	
	int FIN_FCT = 0;
		// permet de savoir si on l'on est arrivé à la fin d'une fonction
		// et si l'on a rencontré un "return" à la fin de cette fonction
	
	Stack<Integer> stack_expr = new Stack<>();
	Stack<Integer> stack_block = new Stack<>();
	
	Stack<Expr> stack_addr_expr = new Stack<>();
		// permet de garder une trace des différentes expressions créées dans une fonction
	Stack<Sblock> stack_addr_block = new Stack<>();
		// permet de garder une trace des différents blocs créés dans une fonction
	Stack<Stmt> stack_addr_stmt = new Stack<>();
		// permet de garder une trace des différentes instructions créées dans une fonction
	Stack<Integer> stack_struct = new Stack<>();
		// permet de savoir si on s'occupe d'une variable liée à une structure ou non
		// si la tête de pile est à 0, alors l'on est définitivement sur une variable
		// non liée à une structure
	
	String name_struct;
	
	int nb_main = 0;
		// compte le nombre de fonctions "main" dans un programme
	
	boolean debug = false;
	String type_field;
	HashMap<String, Structure> structure;

  // et renvoyé par cette fonction
  File getFile() {
    if (file == null) throw new Error("typing not yet done!");
    return file;
  }

  // il faut compléter le visiteur ci-dessous pour réaliser le typage

  public String error_message(int l, int c, String s) {
    return String.format("line %d, column %d:\n%s", l, c, s);
  }

  @Override
  public void visit(Pfile n) {
    file = new File(new LinkedList<Decl_fun>());
    stack_struct.add(0);
    structure = new HashMap<>();
    for (Pdecl tmp : n.l) {
      if (tmp instanceof Pstruct) {
        this.visit((Pstruct) tmp);
      } else if (tmp instanceof Pfun) {
        this.visit((Pfun) tmp);
      } else {
        throw new Error("Visit Pfile");
      }
    }
    if (nb_main == 0) throw new Error("main function not found");
  }

  @Override
  public void visit(Pident n) {
    // if (debug) System.out.println("Pident Deb");

    if (stack_struct.peek() == 0) {
      if (cursor == 0 && stack_struct.peek() == 0) {
        for (Decl_fun funs : file.funs) {
          if (
            funs.fun_name != null && funs.fun_name.equals(n.id)
          ) throw new Error(
            String.format(
              "line %d, column %d:\nredefinition of function \"%s\"",
              n.loc.line,
              n.loc.column,
              n.id
            )
          );
        }
        file.funs.getLast().fun_name = n.id;
        if (n.id.equals("main")) nb_main++;
        if (nb_main > 1) throw new Error(
          String.format(
            "line %d, column %d:\nMore than one function named main (%d)",
            n.loc.line,
            n.loc.column,
            nb_main
          )
        );
      } else if (cursor <= nb_args && stack_struct.peek() == 0) {
        for (Decl_var var : file.funs.getLast().fun_formals) {
          if (var.name != null && var.name.equals(n.id)) throw new Error(
            String.format(
              "line %d, column %d:\nredefinition of variable \"%s\"",
              n.loc.line,
              n.loc.column,
              n.id
            )
          );
        }
        file.funs.getLast().fun_formals.getLast().name = n.id;
      } else {
        Sblock b = stack_addr_block.pop();
        stack_addr_block.push(b);

        ident_block = stack_block.peek();

        switch (ident_block) {
          case IDENT_DECL:
        	  // lorsqu'on est au niveau de la déclaration d'une variable
            for (Decl_var var : b.dl) {
              if (var.name != null && var.name.equals(n.id)) {
                throw new Error(
                  String.format(
                    "line %d, column %d:\nredefinition of variable \"%s\"",
                    n.loc.line,
                    n.loc.column,
                    n.id
                  )
                );
              }
            }
            b.dl.getLast().name = n.id;
            TYP_FIELD = b.dl.getLast().t;
            break;
          default:
        	  // dans ce cas, la variable est censé déjà être déclarée comme  
        	  // une variable locale où un argument de fonction
            int present = 0;
            for (int i = stack_addr_block.size() - 1; i >= 0; i--) {
              Sblock blc = stack_addr_block.get(i);
              if (blc == null) break;
              for (Decl_var var : blc.dl) {
                if (var.name.equals(n.id)) {
                  present = 1;
                  TYP_FIELD = var.t;
                  break;
                }
              }
            }

            if (present == 0) {
              for (Decl_var var : file.funs.getLast().fun_formals) {
                if (var.name.equals(n.id)) {
                  present = 1;
                  TYP_FIELD = var.t;
                  break;
                }
              }
            }
            if (present == 0) throw new Error(
              String.format(
                "line %d, column %d:\nunknown variable \"%s\"",
                n.loc.line,
                n.loc.column,
                n.id
              )
            );
            stack_addr_expr.add(new Eaccess_local(n.id));
        }
      }
    } else if (stack_struct.peek() == STRUCT_NAME) {
    	// si la variable à visiter est le nom d'une structure
      structure.put(n.id, new Structure(n.id));
      name_struct = n.id;
    } else if (stack_struct.peek() == STRUCT_FIELD) {
    	// si la variable à visiter est un champ d'une structure
      if (structure.get(name_struct).fields.containsKey(n.id)) {
        throw new Error(
          String.format(
            "line %d, column %d:\nredefinition of variable \"%s\"",
            n.loc.line,
            n.loc.column,
            n.id
          )
        );
      }

      int current_size = structure.get(name_struct).fields.size();
      if (type_field.equals("0")) structure
        .get(name_struct)
        .fields.put(
          n.id,
          new Field(n.id, new Tint(), current_size)
        ); else structure
        .get(name_struct)
        .fields.put(
          n.id,
          new Field(n.id, new Tstructp(structure.get(type_field)), current_size)
        );
    } else if (stack_struct.peek() == STRUCT_VARNAME) {
    	// si la variable à visiter est de type struct
    	Sblock b = stack_addr_block.pop();
    	b.dl.getLast().name = n.id;
    	stack_addr_block.push(b);
    }

    // if (debug) System.out.println("Pident Fin");
  }

  @Override
  public void visit(PTint n) {
    // if (debug) System.out.println("Ptint deb ");
    if (stack_struct.peek() == 0) {
      if (cursor == 0) file.funs.add(
        new Decl_fun(new Tint(), null, null, null)
      ); else if (cursor <= nb_args) {
        file.funs.getLast().fun_formals.add(new Decl_var(new Tint(), null));
      } else {
        Sblock b = stack_addr_block.pop();
        b.dl.add(new Decl_var(new Tint(), null));
        stack_addr_block.push(b);
      }
    } else if (stack_struct.peek() == STRUCT_FIELD) {
      type_field = "0";
    }
    // if (debug) System.out.println("Ptint Fin");
  }

  @Override
  public void visit(PTstruct n) {
    //if (debug) System.out.println("Deb PTstruct");
    Structure s;
    if (stack_struct.peek() == 0) {
      if (cursor == 0) {
    	  // si l'on visite une fonction renvoyant une structure
        s = structure.get(n.id);
        if (s == null) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              "no such struct \"" + n.id + "\""
            )
          );
        } else {
          file.funs.add(new Decl_fun(new Tstructp(s), null, null, null));
        }
      } else if (cursor <= nb_args) {
    	 // si l'on visite un argument de fonction de type structure
        s = structure.get(n.id);
        if (s == null) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              "no such struct \"" + n.id + "\""
            )
          );
        } else {
          file.funs
            .getLast()
            .fun_formals.add(new Decl_var(new Tstructp(s), null));
        }
      } else {
    	// si l'on visite une variable locale de type structure
        s = structure.get(n.id);
        if (s == null) throw new Error(
          error_message(
            n.loc.line,
            n.loc.column,
            "no such struct \"" + n.id + "\""
          )
        );

        Sblock b = stack_addr_block.pop();
        b.dl.add(new Decl_var(new Tstructp(structure.get(n.id)), null));
        stack_addr_block.push(b);
      }
    } else if (stack_struct.peek() == STRUCT_FIELD) {
    	// si l'on visite un champ d'une structure
      if (structure.get(n.id) == null) {
        throw new Error(
          error_message(
            n.loc.line,
            n.loc.column,
            "undefined structure \"" + n.id + "\""
          )
        );
      }
      type_field = n.id;
    }
    //if (debug) System.out.println("Fin PTstruct");
  }

  @Override
  public void visit(Pint n) {
    //if (debug) System.out.println("Deb Pint");
    stack_addr_expr.push(new Econst(n.n));
    TYP_FIELD = new Tint();
    //if (debug) System.out.println("Fin Pint");
  }

  @Override
  public void visit(Punop n) {
    //if (debug) System.out.println("Eunop Deb");
    Sblock temp = stack_addr_block.peek();

    if (temp != null) {
      if (n.e1 instanceof Pident) {
        for (Decl_var var : temp.dl) {
        	// on interdit une operation du style: "-s;" où s est une structure
          if (
            var.t instanceof Tstructp &&
            var.name.equals(((Pident) n.e1).id) &&
            n.op.toString().equals("Uneg")
          ) {
            throw new Error(
              error_message(
                n.loc.line,
                n.loc.column,
                "incompatible type operation"
              )
            );
          }
        }
      }
    } else {
      throw new Error(
        error_message(n.loc.line, n.loc.column, "incompatible type operation")
      );
    }

    stack_expr.push(IDENT_EUNOP);
    Expr e = new Eunop(n.op, null);
    stack_addr_expr.push(e);
    this.visit(n.e1);
    ((Eunop) e).e = stack_addr_expr.pop();
    stack_expr.pop();
    
    //if (debug) System.out.println("Eunop Fin");
  }

  @Override
  public void visit(Passign n) {
    //if (debug) System.out.println("Passign Deb");
    stack_expr.push(IDENT_PASSIGN);
    Expr e;
    Typ t1 = null, t2 = null;
    if (n.e1 instanceof Parrow) {
      e = new Eassign_field(null, null, null);

      this.visit(n.e1);

      if (TYP_FIELD instanceof Tint) t1 = new Tint(); else if (
        TYP_FIELD instanceof Tstructp
      ) t1 = new Tstructp(structure.get(((Tstructp) TYP_FIELD).s.str_name));

      ((Eassign_field) e).e1 = stack_addr_expr.pop();

      if (TYP_FIELD instanceof Tint) {
        ((Eassign_field) e).f = new Field(NAME_FIELD, new Tint(), -1);
      } else if (TYP_FIELD instanceof Tstructp) {
        ((Eassign_field) e).f =
          new Field(
            NAME_FIELD,
            new Tstructp(structure.get(((Tstructp) TYP_FIELD).s.str_name)),
            -1
          );
      }

      ((Eassign_field) e).f.pos =
        structure.get(ORIGIN_TYP).fields.get(NAME_FIELD).pos;

      this.visit(n.e2);
      if (TYP_FIELD instanceof Tint) t2 = new Tint(); else if (
        TYP_FIELD instanceof Tstructp
      ) t2 = new Tstructp(structure.get(((Tstructp) TYP_FIELD).s.str_name));

      ((Eassign_field) e).e2 = stack_addr_expr.pop();
      if (t1 instanceof Tint && t2 instanceof Tstructp) {
        throw new Error(
          error_message(n.loc.line, n.loc.column, "incompatible type operation")
        );
      }
      if (t1 instanceof Tstructp && t2 instanceof Tstructp) {
        if (((Tstructp) t1).s.str_name != ((Tstructp) t2).s.str_name) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              "incompatible type operation"
            )
          );
        }
      }
      if (t2 instanceof Tint && t1 instanceof Tstructp) {
        if (
          !(
            ((Eassign_field) e).e2 instanceof Econst &&
            ((Econst) ((Eassign_field) e).e2).i == 0
          )
        ) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              "incompatible type operation"
            )
          );
        }
      }

      stack_addr_expr.push(e);
    } else {
      this.visit(((Pident) n.e1));
      if (TYP_FIELD instanceof Tint) t1 = new Tint(); else if (
        TYP_FIELD instanceof Tstructp
      ) t1 = new Tstructp(structure.get(((Tstructp) TYP_FIELD).s.str_name));

      stack_addr_expr.pop();
      e = new Eassign_local(((Pident) n.e1).id, null);
      this.visit(n.e2);
      if (TYP_FIELD instanceof Tint) t2 = new Tint(); else if (
        TYP_FIELD instanceof Tstructp
      ) t2 = new Tstructp(structure.get(((Tstructp) TYP_FIELD).s.str_name));

      ((Eassign_local) e).e = stack_addr_expr.pop();

      if (t1 instanceof Tint && t2 instanceof Tstructp) {
        throw new Error(
          error_message(n.loc.line, n.loc.column, "incompatible type operation")
        );
      }
      if (t1 instanceof Tstructp && t2 instanceof Tstructp) {
        if (((Tstructp) t1).s.str_name != ((Tstructp) t2).s.str_name) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              "incompatible type operation"
            )
          );
        }
      }
      if (t2 instanceof Tint && t1 instanceof Tstructp) {
        if (
          !(
            ((Eassign_local) e).e instanceof Econst &&
            ((Econst) ((Eassign_local) e).e).i == 0
          )
        ) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              "incompatible type operation"
            )
          );
        }
      }

      stack_addr_expr.push(e);
    }
    stack_expr.pop();
    // if (debug) System.out.println("Passign Fin");
  }

  @Override
  public void visit(Pbinop n) {
    //if (debug) System.out.println("Binop Deb");

    Expr e = new Ebinop(n.op, null, null);
    Typ t1 = null, t2 = null;

    stack_expr.push(IDENT_EBINOP);
    ident_block = stack_block.peek();

    this.visit(n.e1);
    if (TYP_FIELD instanceof Tint) t1 = new Tint(); else if (
      TYP_FIELD instanceof Tstructp
    ) t1 = new Tstructp(structure.get(((Tstructp) TYP_FIELD).s.str_name));
    ((Ebinop) e).e1 = stack_addr_expr.pop();

    this.visit(n.e2);
    if (TYP_FIELD instanceof Tint) t2 = new Tint(); else if (
      TYP_FIELD instanceof Tstructp
    ) t2 = new Tstructp(structure.get(((Tstructp) TYP_FIELD).s.str_name));
    ((Ebinop) e).e2 = stack_addr_expr.pop();

    if (t1 instanceof Tint && t2 instanceof Tstructp) {
      throw new Error(
        error_message(n.loc.line, n.loc.column, "incompatible type operation")
      );
    }
    if (t1 instanceof Tstructp && t2 instanceof Tstructp) {
      if (((Tstructp) t1).s.str_name != ((Tstructp) t2).s.str_name) {
        throw new Error(
          error_message(
            n.loc.line,
            n.loc.column,
            "incompatible type operation at location "
          )
        );
      }
      if (
        n.op.toString().equals("Beq") == false &&
        n.op.toString().equals("Bneq") == false
      ) {
        throw new Error(
          error_message(
            n.loc.line,
            n.loc.column,
            "incompatible type operation at location "
          )
        );
      }
    }

    stack_addr_expr.push(e);
    stack_expr.pop();
    //if (debug) System.out.println("Binop Fin");
  }

  @Override
  public void visit(Parrow n) {
    //if (debug) System.out.println("Parrow Deb");
    Expr e = new Eaccess_field(null, new Field(n.f, null, -1));

    this.visit(n.e);

    int present = 0;
    Expr f = stack_addr_expr.peek();

    if (f instanceof Eaccess_local) {
      for (int i = stack_addr_block.size() - 1; i >= 0; i--) {
        Sblock blc = stack_addr_block.get(i);
        if (blc == null) break;
        for (Decl_var var : blc.dl) {
          if (
            var.name.equals(((Eaccess_local) f).i) && var.t instanceof Tstructp
          ) {
            present = 1;
            if (
              structure.get(((Tstructp) var.t).s.str_name).fields.get(n.f) ==
              null
            ) {
              throw new Error(
                error_message(
                  n.loc.line,
                  n.loc.column,
                  String.format(
                    "structure %s don't have field %s",
                    ((Tstructp) var.t).s.str_name,
                    n.f
                  )
                )
              );
            }
            TYP_FIELD =
              structure
                .get(((Tstructp) var.t).s.str_name)
                .fields.get(n.f)
                .field_typ;
            NAME_FIELD = n.f;
            ORIGIN_TYP = structure.get(((Tstructp) var.t).s.str_name).str_name;
            break;
          }
        }
      }
      if (present == 0) {
        for (Decl_var var : file.funs.getLast().fun_formals) {
          if (
            var.name.equals(((Eaccess_local) f).i) && var.t instanceof Tstructp
          ) {
            present = 1;
            if (
              structure.get(((Tstructp) var.t).s.str_name).fields.get(n.f) ==
              null
            ) {
              throw new Error(
                error_message(
                  n.loc.line,
                  n.loc.column,
                  String.format(
                    "structure %s don't have field %s",
                    ((Tstructp) var.t).s.str_name,
                    n.f
                  )
                )
              );
            }
            TYP_FIELD =
              structure
                .get(((Tstructp) var.t).s.str_name)
                .fields.get(n.f)
                .field_typ;
            NAME_FIELD = n.f;
            ORIGIN_TYP = structure.get(((Tstructp) var.t).s.str_name).str_name;
            break;
          }
        }
      }
      if (present == 0) {
        throw new Error(
          error_message(
            n.loc.line,
            n.loc.column,
            String.format("not a structure \"%s\"", n.f)
          )
        );
      }
    } else if (f instanceof Eaccess_field) {
      Typ T2 = TYP_FIELD;
      TYP_FIELD =
        structure.get(((Tstructp) T2).s.str_name).fields.get(n.f).field_typ;
      NAME_FIELD = n.f;
      ORIGIN_TYP = structure.get(((Tstructp) T2).s.str_name).str_name;
    } else {
      throw new Error(
        error_message(
          n.loc.line,
          n.loc.column,
          String.format("not a structure \"%s\"", n.f)
        )
      );
    }
    ((Eaccess_field) e).e = stack_addr_expr.pop();
    ((Eaccess_field) e).f.field_typ = TYP_FIELD;
    ((Eaccess_field) e).f.pos = structure.get(ORIGIN_TYP).fields.get(n.f).pos;
    stack_addr_expr.push(e);

    //if (debug) System.out.println("Parrow Fin");
  }

  @Override
  public void visit(Pcall n) {
    //if (debug) System.out.println("Pcall Deb");

    int cmp = 0;
    for (Decl_fun funs : file.funs) {
      if (funs.fun_name.equals(n.f)) break;
      cmp++;
    }
    if (
      cmp == file.funs.size() &&
      n.f.equals("putchar") == false &&
      n.f.equals("sbrk") == false
    ) {
      throw new Error(
        error_message(
          n.loc.line,
          n.loc.column,
          String.format("function \"%s\" not found", n.f)
        )
      );
    }
    int taille = (cmp == file.funs.size()) ? 1 : n.l.size();
    // soit la taille est celle donée par le Pcall, soit la taille est 1,
    // dans le cas où l'on considère qu'on a affaire aux fonctions 
    // "sbrk" où "putchar"
    if (
      cmp < file.funs.size() && file.funs.get(cmp).fun_formals.size() != taille
    ) {
      throw new Error(
        error_message(
          n.loc.line,
          n.loc.column,
          String.format(
            "bad arguments number \"%s\" get %d arguments",
            n.f,
            n.l.size()
          )
        )
      );
    } else if (cmp == file.funs.size() && n.l.size() != 1) {
      throw new Error(
        error_message(
          n.loc.line,
          n.loc.column,
          String.format(
            "bad arguments number \"%s\" get %d arguments",
            n.f,
            n.l.size()
          )
        )
      );
    } else {
      Expr e;
      stack_block.push(IDENT_PEVAL);
      stack_expr.push(IDENT_ECALL);
      e = new Ecall(n.f, new LinkedList<>());
      stack_addr_expr.push(e);
      if (n.f.equals("sbrk")) {
        this.visit(n.l.get(0));
        // on vérifie que le type de l'argument de sbrk est bien Esizeof
        if (!(stack_addr_expr.peek() instanceof Esizeof)) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              String.format("bad argument type\"%s\"", n.f)
            )
          );
        }

        Expr f = stack_addr_expr.pop();
        e = stack_addr_expr.pop();
        ((Ecall) e).el.add(f);
        stack_addr_expr.push(e);
      } else if (n.f.equals("putchar")) {
        this.visit(n.l.get(0));
        // on vérifie que le type de l'argument de sbrk est bien Tint
        if (!(TYP_FIELD instanceof Tint)) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              String.format("bad argument type\"%s\"", n.f)
            )
          );
        }

        Expr f = stack_addr_expr.pop();
        e = stack_addr_expr.pop();
        ((Ecall) e).el.add(f);
        stack_addr_expr.push(e);
      } else {
        for (int i = 0; i < taille; i++) {
          Pexpr vars = n.l.get(i);
          this.visit(vars);
          
          // verification des types des arguments
          if (
            file.funs.get(cmp).fun_formals.get(i).t instanceof Tint &&
            TYP_FIELD instanceof Tstructp
          ) {
            throw new Error(
              error_message(
                n.loc.line,
                n.loc.column,
                String.format("bad argument type \"%s\"", n.f)
              )
            );
          }
          if (
            file.funs.get(cmp).fun_formals.get(i).t instanceof Tstructp &&
            TYP_FIELD instanceof Tint
          ) {
            Expr temp = stack_addr_expr.peek();
            if (!(temp instanceof Econst && ((Econst) temp).i == 0)) {
              throw new Error(
                error_message(
                  n.loc.line,
                  n.loc.column,
                  String.format("bad argument type \"%s\"", n.f)
                )
              );
            }
          }
          if (
            file.funs.get(cmp).fun_formals.get(i).t instanceof Tstructp &&
            TYP_FIELD instanceof Tstructp
          ) {
            Typ t2 = file.funs.get(cmp).fun_formals.get(i).t;
            if (
              ((Tstructp) t2).s.str_name != ((Tstructp) TYP_FIELD).s.str_name
            ) {
              throw new Error(
                error_message(
                  n.loc.line,
                  n.loc.column,
                  String.format("bad argument type \"%s\"", n.f)
                )
              );
            }
          }
          Expr f = stack_addr_expr.pop();

          e = stack_addr_expr.pop();
          ((Ecall) e).el.add(f);
          stack_addr_expr.push(e);
        }
        TYP_FIELD = file.funs.get(cmp).fun_typ;
      }
      stack_block.pop();
      stack_expr.pop();

      //if (debug) System.out.println("Pcall Fin");
    }
  }

  @Override
  public void visit(Psizeof n) {
    //if (debug) System.out.println("Psizeof Deb");
    if (structure.get(n.id) == null) {
	  throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              String.format("sizeof of a structure \"%s\" not found", n.id)
            )
          );
    }
    Expr e = new Esizeof(structure.get(n.id));
    stack_addr_expr.push(e);
    //if (debug) System.out.println("Psizeof Fin");
  }

  @Override
  public void visit(Pskip n) {
    //if (debug) System.out.println("Pskip Deb");
    stack_addr_stmt.push(new Sskip());
    //if (debug) System.out.println("Pskip Fin");
  }

  @Override
  public void visit(Peval n) {
    //if (debug) System.out.println("Peval Deb");
    stack_block.push(IDENT_PEVAL);
    Pexpr tmp = n.e;
    Expr e = null;
    stack_addr_expr.push(e);
    this.visit(tmp);
    stack_addr_stmt.add(new Sexpr(stack_addr_expr.pop()));
    stack_block.pop();
    //if (debug) System.out.println("Peval Fin");
  }

  @Override
  public void visit(Pif n) {
    //if (debug) System.out.println("Pif Deb");
    stack_block.push(IDENT_SIF);
    Pexpr tmp = n.e;
    int nb_return = 0;
    Sif b = new Sif(null, null, null);
    stack_addr_expr.push(null);
    
    this.visit(tmp);
    b.e = stack_addr_expr.pop();
    
    if (n.s1 instanceof Pbloc) {
      this.visit(n.s1);
      b.s1 = stack_addr_block.pop();
      if (
        nb_pblocs == 1 &&
        !((Sblock) b.s1).sl.isEmpty() &&
        ((Sblock) b.s1).sl.getLast() instanceof Sreturn
      ) nb_return++;
    } else {
      this.visit(n.s1);
      b.s1 = stack_addr_stmt.pop();
      if (nb_pblocs == 1 && b.s1 instanceof Sreturn) nb_return++;
    }

    if (n.s2 instanceof Pbloc) {
      this.visit(n.s2);
      b.s2 = stack_addr_block.pop();
      if (
        nb_pblocs == 1 &&
        !((Sblock) b.s2).sl.isEmpty() &&
        ((Sblock) b.s2).sl.getLast() instanceof Sreturn
      ) nb_return++;
    } else {
      this.visit(n.s2);
      b.s2 = stack_addr_stmt.pop();
      if (nb_pblocs == 1 && b.s2 instanceof Sreturn) nb_return++;
    }

    if (nb_return == 2) FIN_FCT = 1;
    	// si le bloc principal de la fonction contient if bloc if-else et que
    	// ces 2 blocs "if" et "else" sont terminés par return, l'on met FIN_FCT à 1
    	// indiquant ainsi la fin de la fonction

    stack_addr_stmt.push(b);
    stack_block.pop();
    if (debug) System.out.println("Pif fin");
  }

  @Override
  public void visit(Pwhile n) {
    //if (debug) System.out.println("Pwhile Deb");
    stack_block.push(IDENT_SWHILE);

    Swhile b = new Swhile(null, null);
    stack_addr_expr.push(null);
    
    this.visit(n.e);
    b.e = stack_addr_expr.pop();

    this.visit(n.s1);
    if (n.s1 instanceof Pbloc) {
    	b.s = stack_addr_block.pop(); 
    }
    else {
    	b.s = stack_addr_stmt.pop();
    }

    stack_addr_stmt.push(b);
    stack_block.pop();
    
    //if (debug) System.out.println("Pwhile Fin");
  }

  @Override
  public void visit(Pbloc n) {
    //if (debug) System.out.println("Pbloc deb");
    nb_pblocs++;

    int temp;
    try {
      temp = stack_block.peek();
    } catch (EmptyStackException e) {
      temp = -1;
    }
    stack_block.push(IDENT_DECL);

    Sblock b = new Sblock(new LinkedList<Decl_var>(), new LinkedList<Stmt>());
    stack_addr_block.push(b);

    // on visite d'abord les différentes déclarations
    for (Pdeclvar tmp : n.vl) {
      if (tmp.typ instanceof PTint) this.visit((PTint) tmp.typ); else if (
        tmp.typ instanceof PTstruct
      ) this.visit((PTstruct) tmp.typ); else throw new Error(
        "visit Pfun Pdeclvar " + tmp.loc
      );
      this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
    }
    stack_block.pop();

    int rreturn = 0;
    
    // on visite ensuite les différentes instructions
    for (Pstmt tmp : n.sl) {
      if (FIN_FCT == 1) {
        throw new Error(
          error_message(
            n.loc.line,
            n.loc.column,
            String.format("unreachable code")
          )
        );
      }
      if (rreturn == 0) {
        if (tmp instanceof Preturn) {
          rreturn = 1;
          if (
            nb_pblocs == 1 && temp != IDENT_SIF && temp != IDENT_SWHILE
          ) FIN_FCT = 1;
        }
        this.visit(tmp);
        if (tmp instanceof Pbloc) {
          b.sl.add(stack_addr_block.pop());
        } else {
          b.sl.add(stack_addr_stmt.pop());
        }
      } else {
        throw new Error(
          error_message(
            n.loc.line,
            n.loc.column,
            String.format("unreachable code")
          )
        );
      }
    }

    nb_pblocs--;
    //if (debug) System.out.println("Pbloc fin");
  }

  @Override
  public void visit(Preturn n) {
    //if (debug) System.out.println("Preturn deb");
    stack_block.push(IDENT_SRETURN);

    stack_addr_expr.push(null);
    this.visit(n.e);
    stack_addr_stmt.add(new Sreturn(stack_addr_expr.pop()));

    stack_block.pop();
    //if (debug) System.out.println("Preturn fin");
  }

  @Override
  public void visit(Pstruct n) {
    //if (debug) System.out.println("Pstruct Deb");

    stack_struct.add(STRUCT_NAME);
    this.visit(new Pident(new Pstring(n.s, n.fl.getFirst().loc)));
    stack_struct.pop();

    for (Pdeclvar tmp : n.fl) {
      type_field = "0";
      stack_struct.add(STRUCT_FIELD);

      if (tmp.typ instanceof PTint) {
        this.visit((PTint) tmp.typ);
      } else if (tmp.typ instanceof PTstruct) {
        this.visit((PTstruct) tmp.typ);
      } else {
		throw new Error(
			error_message(
			  tmp.loc.line,
			  tmp.loc.column,
			  String.format("unknown type of variable \"%s\"", tmp.id)
			)
		  );
      }
      this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
      stack_struct.pop();
    }

    //if (debug) System.out.println("Pstruct Fin");
  }

  @Override
  public void visit(Pfun n) {
    FIN_FCT = 0;
    nb_pblocs = 0;
    Ptype ty = n.ty;
    nb_args = n.pl.size();

    if (ty instanceof PTint) {
      this.visit((PTint) ty);
    } else if (ty instanceof PTstruct) {
      this.visit((PTstruct) ty);
    } else {
		
		throw new Error(
			error_message(
			  n.loc.line,
			  n.loc.column,
			  String.format("unknown type of function \"%s\"", n.s)
			)
		  );
    }
    this.visit(new Pident(new Pstring(n.s, n.loc)));

    cursor = 1;
    file.funs.getLast().fun_formals = new LinkedList<>();
    for (Pdeclvar tmp : n.pl) {
      if (tmp.typ instanceof PTint) {
        this.visit((PTint) tmp.typ);
        this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
      } else if (tmp.typ instanceof PTstruct) {
        this.visit((PTstruct) tmp.typ);
        this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
      } else {
        throw new Error(
			error_message(
			  tmp.loc.line,
			  tmp.loc.column,
			  String.format("unknown type of variable \"%s\"", tmp.id)
			)
		  );
      }
      cursor++;
    }

    stack_addr_stmt.push(null);
    this.visit(n.b);
    try {
      file.funs.getLast().fun_body = stack_addr_block.pop();
    } catch (EmptyStackException e) {}
    cursor = 0;
    if (
      file.funs.getLast().fun_typ instanceof Tint ||
      file.funs.getLast().fun_typ instanceof Tstructp
    ) {
      String fun_name = file.funs.getLast().fun_name;
      if (FIN_FCT == 0) {
        if (!fun_name.equals("main")) {
          throw new Error(
            error_message(
              n.loc.line,
              n.loc.column,
              String.format("return of function \"%s\" not found ", fun_name)
            )
          );
        } else {
          ((Sblock) file.funs.getLast().fun_body).sl.add(
              new Sreturn(new Econst(0))
            );
        }
      }
    }
    stack_addr_block.push(null);
  }

  public void visit(Pexpr tmp) {
    if (tmp instanceof Pint) {
      this.visit((Pint) tmp);
    } else if (tmp instanceof Pident) {
      this.visit((Pident) tmp);
    } else if (tmp instanceof Parrow) {
      this.visit((Parrow) tmp);
    } else if (tmp instanceof Plvalue) {
      this.visit((Plvalue) tmp);
    } else if (tmp instanceof Passign) {
      this.visit((Passign) tmp);
    } else if (tmp instanceof Pbinop) {
      this.visit((Pbinop) tmp);
    } else if (tmp instanceof Punop) {
      this.visit((Punop) tmp);
    } else if (tmp instanceof Pcall) {
      this.visit((Pcall) tmp);
    } else if (tmp instanceof Psizeof) {
      this.visit((Psizeof) tmp);
    }
  }

  public void visit(Pstmt tmp) {
    if (tmp instanceof Pbloc) {
      this.visit((Pbloc) tmp);
    } else if (tmp instanceof Pskip) {
      this.visit((Pskip) tmp);
    } else if (tmp instanceof Preturn) {
      this.visit((Preturn) tmp);
    } else if (tmp instanceof Pif) {
        this.visit((Pif) tmp);
    } else if (tmp instanceof Peval) {
      this.visit((Peval) tmp);
    } else if (tmp instanceof Pwhile) {
      this.visit((Pwhile) tmp);
    }
  }
}
