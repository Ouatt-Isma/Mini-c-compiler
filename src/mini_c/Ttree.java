package mini_c;

import java.util.HashMap;
import java.util.LinkedList;

abstract class Typ {
	abstract void accept(Visitor v);
}

class Tint extends Typ {
	Tint() {}

	void accept(Visitor v) {
		v.visit(this);
	}
	@Override
	public String toString() {
	  return "int";
	}
}

class Tstructp extends Typ {
	public Structure s;

	Tstructp(Structure s) {
		this.s = s;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	@Override
	public String toString() {
	  return "struct " + s.str_name + "*";
	}
}

class Tvoidstar extends Typ {
	Tvoidstar() {
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	@Override
	public String toString() {
	  return "void*";
	}
}

class Ttypenull extends Typ {
	Ttypenull() {
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	@Override
	public String toString() {
	  return "typenull";
	}
}

class Structure {
	public String str_name;
	public HashMap<String, Field> fields;
	// on pourra ajouter plus tard ici la taille totale de la structure
	

	Structure(String str_name) {
		this.str_name = str_name;
		this.fields = new HashMap<String, Field>();
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return this.str_name +":"+this.fields;
	}
}

class Field {
	public String field_name;
	public Typ field_typ;
	// on pourra ajouter plus tard ici la position du champ dans la structure
	public int pos;

	Field(String field_name, Typ field_typ, int pos) {
		this.field_name = field_name;
		this.field_typ = field_typ;
		this.pos = pos;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "["+this.field_name +":"+this.field_typ+":"+this.pos+"]";
	}
}

class Decl_var {
	public Typ t;
	public String name;

	Decl_var(Typ t, String i) {
		this.t = t;
		this.name = i;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	
	@Override
	public String toString() {
	  return t.toString() + " " + name;
	}
}

// expression

abstract class Expr {
	public Typ typ; // chaque expression est décorée par son type
	
	abstract void accept(Visitor v);
	
	
}

class Econst extends Expr {
	public int i;

	Econst(int i) {
		this.i = i;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	//US
	public String toString() {
		return "Econst: "+this.i; 
	}
	//
}

class Eaccess_local extends Expr {
	public String i;

	Eaccess_local(String i) {
		this.i = i;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	//US
		public String toString() {
			return "Eaccess_local: "+this.i; 
		}
	//
}

class Eaccess_field extends Expr {
	public Expr e;
	public Field f;

	Eaccess_field(Expr e, Field f) {
		this.e = e;
		this.f = f;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	//US
	public String toString() {
		return "Eaccess_field: " + this.e + " -> " + this.f; 
	}
	//
}

class Eassign_local extends Expr {
	public String i;
	public Expr e;

	Eassign_local(String i, Expr e) {
		this.i = i;
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	
	//US
			public String toString() {
				return "Eassign_local: "+this.i +" "+this.e; 
			}
		//
}

class Eassign_field extends Expr {
	public Expr e1;
	public Field f;
	public Expr e2;

	Eassign_field(Expr e1, Field f, Expr e2) {
		this.e1 = e1;
		this.f = f;
		this.e2 = e2;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	//US
	public String toString() {
		return "Eassign_field: "+this.e1 + " -> " + this.f + " = "+this.e2; 
	}
//
}

class Eunop extends Expr {
	public Unop u;
	public Expr e;

	Eunop(Unop u, Expr e) {
		this.u = u;
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	//US
		public String toString() {
	return "Eunop: "+ this.u + " " + this.e; 
		}
		//
}

class Ebinop extends Expr {
	public Binop b;
	public Expr e1;
	public Expr e2;

	Ebinop(Binop b, Expr e1, Expr e2) {
		this.b = b;
		this.e1 = e1;
		this.e2 = e2;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	//US
			public String toString() {
		return "(Ebinop: "+this.e1 +" "+ this.b+" "+ " "+ this.e2+")"; 
			}
			//
}

class Ecall extends Expr {
	public String i;
	public LinkedList<Expr> el;

	Ecall(String i, LinkedList<Expr> el) {
		this.i = i;
		this.el = el;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "(Ecall: "+this.i +" "+ this.el+")"; 
	}
}

class Esizeof extends Expr {
	public Structure s;

	Esizeof(Structure s) {
		this.s = s;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "(Esizeof: " + this.s.str_name + ")"; 
	}
}

// instruction

abstract class Stmt {
	abstract void accept(Visitor v);
}

class Sskip extends Stmt {
	Sskip() {
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "Sskip"; 
	}
}

class Sexpr extends Stmt {
	public Expr e;

	Sexpr(Expr e) {
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "Sexpr: "+ this.e; 
	}
}

class Sif extends Stmt {
	public Expr e;
	public Stmt s1;
	public Stmt s2;

	Sif(Expr e, Stmt s1, Stmt s2) {
		this.e = e;
		this.s1 = s1;
		this.s2 = s2;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "(Sif: "+this.e + " S1: "+ this.s1+"; S2: "+ " "+ this.s2+")"; 
	}
	
}

class Swhile extends Stmt {
	public Expr e;
	public Stmt s;

	Swhile(Expr e, Stmt s) {
		this.e = e;
		this.s = s;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "(Swhile: "+this.e + " "+ this.s+")"; 
	}

}

class Sblock extends Stmt {
	public LinkedList<Decl_var> dl;
	public LinkedList<Stmt> sl;

	Sblock(LinkedList<Decl_var> dl, LinkedList<Stmt> sl) {
		this.dl = dl;
		this.sl = sl;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		String res; 
		res= "Sblock: \n"; 
		res+="Decl_var: \n"; 
		for(Decl_var d: dl) {
			res+=d.toString() + "\n"; 
		}
		
		res+="Stmt: \n"; 
		for(Stmt s: sl) {
			res+=s.toString() + "\n";
		}
		res+="Sblock fin;\n"; 
		
		return res; 
	}
}

class Sreturn extends Stmt {
	public Expr e;

	Sreturn(Expr e) {
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		return "Sreturn: " + this.e; 
	}
}

// fonction

class Decl_fun {
	public Typ fun_typ;
	public String fun_name;
	public LinkedList<Decl_var> fun_formals;
	public Stmt fun_body;

	Decl_fun(Typ fun_typ, String fun_name, LinkedList<Decl_var> fun_formals,
			Stmt fun_body) {
		this.fun_typ = fun_typ;
		this.fun_name = fun_name;
		this.fun_formals = fun_formals;
		this.fun_body = fun_body;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
}

// programme = liste de fonctions

class File {
	public LinkedList<Decl_fun> funs;

	File(LinkedList<Decl_fun> funs) {
		this.funs = funs;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
	public String toString() {
		String res = "Number of functions " + this.funs.size() + "\n"; 
		
        for(Decl_fun d: this.funs) {
        	res += "\n\n\n\t\t\t";
        	res += d.fun_typ+":" +d.fun_name+"\n" ; 
        	for (Decl_var dd: d.fun_formals){
        		res+="var_type, var_name: "+dd.t + ", "+dd.name+"\n" ; 
        	}
        	if(d.fun_body instanceof Sblock) {
        		Sblock tmp = (Sblock)d.fun_body; 
        		//System.out.println(); 
        		res+= "Deb Sblock"+"\n" ; 
        		for (Decl_var dd: tmp.dl){
            		res+="var_type, var_name: "+dd.t + ", "+dd.name+"\n"; 
            	}
        		for(Stmt sl: tmp.sl) {
        			if(sl instanceof Sreturn) {
        				res+="Sreturn : "+((Sreturn)sl).e+"\n"; 
        			}
        			else if(sl instanceof Sif) {
        				res+="\nSif: "+((Sif)sl).e+"\n"; 
        				res+="\nS1: "+((Sif)sl).s1+"\n"; 
        				res+="S2: "+((Sif)sl).s2+"\n"; 
        				res+= "Fin Sif\n";
        			}
        			else if(sl instanceof Sexpr) {
        				if(((Sexpr)sl).e instanceof Ecall) {
        					res+="Ecall: "+((Ecall)((Sexpr)sl).e).i+ " " + ((Ecall)((Sexpr)sl).e).el +"\n";
        				}
        				else
        					res+="Sexpr: "+((Sexpr)sl).e+"\n";
        			}
        			else if(sl instanceof Sskip) {
        				res+="Sskip\n";
        			}
        			else if(sl instanceof Swhile) {
        				res+="\nSwhile: "+((Swhile)sl).e+"\n"; 
        				res+="\nS: "+((Swhile)sl).s+"\n";  
        				res+= "Fin Swhile\n";
        			}
        			else if(sl instanceof Sblock) {
        				res+="\n"+sl+"\n";
        			}
        			else {
        				res+="sl not instance of Sretrun or Sif"+"\n"; 
        			}
        		}
        		res+="Fin Sblock"+"\n"; 
        	}
        	else {
        		res+="fun_body not instance of sblock"+"\n"; 
        	}
        	System.out.println(); 
        }
        return res; 
	}
	
}

interface Visitor {
	public void visit(Unop n);

	public void visit(Binop n);

	public void visit(String n);

	public void visit(Tint n);

	public void visit(Tstructp n);

	public void visit(Tvoidstar n);

	public void visit(Ttypenull n);

	public void visit(Structure n);

	public void visit(Field n);

	public void visit(Decl_var n);

	public void visit(Expr n);

	public void visit(Econst n);

	public void visit(Eaccess_local n);

	public void visit(Eaccess_field n);

	public void visit(Eassign_local n);

	public void visit(Eassign_field n);

	public void visit(Eunop n);

	public void visit(Ebinop n);

	public void visit(Ecall n);

	public void visit(Esizeof n);

	public void visit(Sskip n);

	public void visit(Sexpr n);

	public void visit(Sif n);

	public void visit(Swhile n);

	public void visit(Sblock n);

	public void visit(Sreturn n);

	public void visit(Decl_fun n);

	public void visit(File n);
}

class EmptyVisitor implements Visitor {
	public void visit(Unop n) {
	}

	public void visit(Binop n) {
	}

	public void visit(String n) {
	}

	public void visit(Tint n) {
	}

	public void visit(Tstructp n) {
	}

	public void visit(Tvoidstar n) {
	}

	public void visit(Ttypenull n) {
	}

	public void visit(Structure n) {
	}

	public void visit(Field n) {
	}

	public void visit(Decl_var n) {
	}

	public void visit(Expr n) {
	}

	public void visit(Econst n) {
	}

	public void visit(Eaccess_local n) {
	}

	public void visit(Eaccess_field n) {
	}

	public void visit(Eassign_local n) {
	}

	public void visit(Eassign_field n) {
	}

	public void visit(Eunop n) {
	}

	public void visit(Ebinop n) {
	}

	public void visit(Ecall n) {
	}

	public void visit(Esizeof n) {
	}

	public void visit(Sskip n) {
	}

	public void visit(Sexpr n) {
	}

	public void visit(Sif n) {
	}

	public void visit(Swhile n) {
	}

	public void visit(Sblock n) {
	}

	public void visit(Sreturn n) {
	}

	public void visit(Decl_fun n) {
	}

	public void visit(File n) {
	}
}


