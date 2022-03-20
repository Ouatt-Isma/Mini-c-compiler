package mini_c;

import java.util.HashSet;

class Lin implements LTLVisitor {
  private LTLgraph cfg; 			// graphe en cours de traduction
  private X86_64 asm; 				// code en cours de construction
  private HashSet<Label> visited; 	// instructions déjà traduites
  
  //private boolean debug = false;

  
  private void lin(Label l) { 
    if (visited.contains(l)) {
      asm.needLabel(l);
      asm.jmp(l.name);
    } else {
      visited.add(l);
      asm.label(l);
      cfg.graph.get(l).accept(this);
    }
  }
  
 
  public void visit(Lload o){
	  //if(debug) System.out.println("Lload Deb");

	  asm.movq(""+o.i+"("+o.r1.toString()+")", o.r2.toString());
	  lin(o.l);
	  
	  //if(debug) System.out.println("Lload Fin");
      
  }
  public void visit(Lstore o){
	  //if(debug) System.out.println("Lstore Deb");
	  
	  asm.movq(o.r1.toString(), ""+o.i+"("+o.r2.toString()+")");
	  lin(o.l);
	  
	  //if(debug) System.out.println("Lstore Fin");
  }
  
  public void visit(Lmubranch o){
	  //if(debug) System.out.println("Lmubranch Deb");
	  
	  if(o.m instanceof Mjz) {
		  if(!visited.contains(o.l2)) {
			  asm.needLabel(o.l2);
		  }
		  asm.cmpq(0, o.r.toString());
		  asm.jne(o.l2.toString());
		  lin(o.l1);
		  lin(o.l2);
	  }
	  else if(o.m instanceof Mjgi) {
		  if (!visited.contains(o.l1)) {
			  asm.needLabel(o.l1);
		  }
		  asm.cmpq(((Mjgi)o.m).n, o.r.toString());
		  asm.jg(o.l1.name);
		  lin(o.l2);
		  lin(o.l1);
	  }
	  else if(o.m instanceof Mjnz) {
		  if(!visited.contains(o.l2)) {
			  asm.needLabel(o.l2);
		  }
		  asm.cmpq(0, o.r.toString());
		  asm.je(o.l2.toString());
		  lin(o.l1);
		  lin(o.l2);
	  }
	  else if(o.m instanceof Mjlei) {
		  if (!visited.contains(o.l1)) {
			  asm.needLabel(o.l1);
		  }
		  asm.cmpq(((Mjlei)o.m).n, o.r.toString());
		  asm.jle(o.l1.name);
		  lin(o.l2);
		  lin(o.l1);
	  }
	  
	  //if(debug) System.out.println("Lmubranch Fin");
  }
  
  public void visit(Lmbbranch o){
	  //if(debug) System.out.println("Lmbbranch Deb");
	  
	  asm.cmpq(o.r1.toString(), o.r2.toString());
	  if (!visited.contains(o.l1)) {
		  asm.needLabel(o.l1);
	  }
	  if(o.m == Mbbranch.Mjle) {
		  asm.jle(o.l1.toString());
	  }
	  else if(o.m == Mbbranch.Mjl) {
		  asm.jl(o.l1.toString());
	  }
	  lin(o.l2);
	  lin(o.l1);
	  
	  //if(debug) System.out.println("Lmbbranch Fin");
  }
  
  public void visit(Lgoto o){
	  //if(debug) System.out.println("Lgoto Deb");    
	  lin(o.l);
	  //if(debug) System.out.println("Lgoto Fin");
  }
  
  public void visit(Lreturn o){
	  //if(debug) System.out.println("Lreturn Deb");
	  asm.ret(); 
      //if(debug) System.out.println("Lreturn Fin");
  }
  
  public void visit(Lconst o){
	  //if(debug) System.out.println("Lconst Deb");
	  asm.movq(o.i, o.o.toString()); 
	  lin(o.l);
	  //if(debug) System.out.println("Lconst Fin");
  }
  
  public void visit(Lmunop o){
	  //if(debug) System.out.println("Lmunop Deb");
	  
	  if(o.m instanceof Maddi) {
		  asm.addq("$"+((Maddi)o.m).n, o.o.toString());
	  }
	  else if(o.m instanceof Msetnei) {
		  asm.cmpq("$"+((Msetnei)o.m).n, o.o.toString());
		  if(o.o.toString().equals("%rax")){
    		  asm.setne("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o.toString().equals("%rdi")){
    		  asm.setne("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o.toString().equals("%rsi")){
    		  asm.setne("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o.toString().equals("%rdx")){
    		  asm.setne("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o.toString().equals("%rcx")){
    		  asm.setne("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o.toString().equals("%rsp")){
    		  asm.setne("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o.toString().equals("%rbx")){
    		  asm.setne("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o.toString().equals("%rbp")){
    		  asm.setne("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o.toString().equals("%r8")){
    		  asm.setne("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o.toString().equals("%r9")){
    		  asm.setne("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o.toString().equals("%r10")) {
    		  asm.setne("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o.toString().equals("%r11")){
    		  asm.setne("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o.toString().equals("%r12")){
    		  asm.setne("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o.toString().equals("%r13")){
    		  asm.setne("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o.toString().equals("%r14")) {
    		  asm.setne("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o.toString().equals("%r15")){
    		  asm.setne("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o instanceof Spilled){
    		  asm.setne(o.o.toString());
    	  }
	  }
	  else if(o.m instanceof Msetei) {
		  asm.cmpq("$"+((Msetei)o.m).n, o.o.toString());
		  if(o.o.toString().equals("%rax")){
    		  asm.sete("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o.toString().equals("%rdi")){
    		  asm.sete("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o.toString().equals("%rsi")){
    		  asm.sete("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o.toString().equals("%rdx")){
    		  asm.sete("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o.toString().equals("%rcx")){
    		  asm.sete("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o.toString().equals("%rsp")){
    		  asm.sete("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o.toString().equals("%rbx")){
    		  asm.sete("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o.toString().equals("%rbp")){
    		  asm.sete("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o.toString().equals("%r8")){
    		  asm.sete("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o.toString().equals("%r9")){
    		  asm.sete("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o.toString().equals("%r10")) {
    		  asm.sete("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o.toString().equals("%r11")){
    		  asm.sete("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o.toString().equals("%r12")){
    		  asm.sete("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o.toString().equals("%r13")){
    		  asm.sete("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o.toString().equals("%r14")) {
    		  asm.sete("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o.toString().equals("%r15")){
    		  asm.sete("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o instanceof Spilled){
    		  asm.sete(o.o.toString());
    	  }
	  }
	  lin(o.l);
	  
	  //if(debug) System.out.println("Lmunop Fin");
  }
  
  public void visit(Lmbinop o){
	  //if(debug) System.out.println("Lmbinop Deb");
      
	  if(o.m == Mbinop.Mmov) {
		  asm.movq(o.o1.toString(), o.o2.toString()); 
	  }
      else if(o.m == Mbinop.Madd) {
    	  asm.addq(o.o1.toString(), o.o2.toString()); 
      }
      else if(o.m == Mbinop.Mdiv) {
    	  asm.cqto();
    	  asm.idivq(o.o1.toString());
      }
      else if(o.m == Mbinop.Mmul) {
    	  asm.imulq(o.o1.toString(), o.o2.toString());
      }
      else if(o.m == Mbinop.Msub) {
    	  asm.subq(o.o1.toString(), o.o2.toString());
      }
      else if(o.m == Mbinop.Msetl) {
    	  asm.cmpq(o.o1.toString(), o.o2.toString());
		  if(o.o2.toString().equals("%rax")){
    		  asm.setl("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o2.toString().equals("%rdi")){
    		  asm.setl("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o2.toString().equals("%rsi")){
    		  asm.setl("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o2.toString().equals("%rdx")){
    		  asm.setl("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o2.toString().equals("%rcx")){
    		  asm.setl("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o2.toString().equals("%rsp")){
    		  asm.setl("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o2.toString().equals("%rbx")){
    		  asm.setl("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o2.toString().equals("%rbp")){
    		  asm.setl("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o2.toString().equals("%r8")){
    		  asm.setl("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o2.toString().equals("%r9")){
    		  asm.setl("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o2.toString().equals("%r10")) {
    		  asm.setl("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o2.toString().equals("%r11")){
    		  asm.setl("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o2.toString().equals("%r12")){
    		  asm.setl("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o2.toString().equals("%r13")){
    		  asm.setl("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o2.toString().equals("%r14")) {
    		  asm.setl("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o2.toString().equals("%r15")){
    		  asm.setl("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o2 instanceof Spilled){
    		  asm.setl(o.o2.toString());
    	  }  
      }
      else if(o.m == Mbinop.Msete) {
    	  asm.cmpq(o.o1.toString(), o.o2.toString());
		  if(o.o2.toString().equals("%rax")){
    		  asm.sete("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o2.toString().equals("%rdi")){
    		  asm.sete("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o2.toString().equals("%rsi")){
    		  asm.sete("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o2.toString().equals("%rdx")){
    		  asm.sete("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o2.toString().equals("%rcx")){
    		  asm.sete("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o2.toString().equals("%rsp")){
    		  asm.sete("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o2.toString().equals("%rbx")){
    		  asm.sete("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o2.toString().equals("%rbp")){
    		  asm.sete("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o2.toString().equals("%r8")){
    		  asm.sete("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o2.toString().equals("%r9")){
    		  asm.sete("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o2.toString().equals("%r10")) {
    		  asm.sete("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o2.toString().equals("%r11")){
    		  asm.sete("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o2.toString().equals("%r12")){
    		  asm.sete("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o2.toString().equals("%r13")){
    		  asm.sete("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o2.toString().equals("%r14")) {
    		  asm.sete("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o2.toString().equals("%r15")){
    		  asm.sete("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o2 instanceof Spilled){
    		  asm.sete(o.o2.toString());
    	  }

      }
      else if(o.m == Mbinop.Msetle) {
    	  asm.cmpq(o.o1.toString(), o.o2.toString());
		  if(o.o2.toString().equals("%rax")){
    		  asm.setle("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o2.toString().equals("%rdi")){
    		  asm.setle("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o2.toString().equals("%rsi")){
    		  asm.setle("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o2.toString().equals("%rdx")){
    		  asm.setle("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o2.toString().equals("%rcx")){
    		  asm.setle("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o2.toString().equals("%rsp")){
    		  asm.setle("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o2.toString().equals("%rbx")){
    		  asm.setle("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o2.toString().equals("%rbp")){
    		  asm.setle("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o2.toString().equals("%r8")){
    		  asm.setle("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o2.toString().equals("%r9")){
    		  asm.setle("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o2.toString().equals("%r10")) {
    		  asm.setle("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o2.toString().equals("%r11")){
    		  asm.setle("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o2.toString().equals("%r12")){
    		  asm.setle("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o2.toString().equals("%r13")){
    		  asm.setle("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o2.toString().equals("%r14")) {
    		  asm.setle("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o2.toString().equals("%r15")){
    		  asm.setle("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o2 instanceof Spilled){
    		  asm.setle(o.o2.toString());
    	  }
      }
      else if(o.m == Mbinop.Msetg) {
    	  asm.cmpq(o.o1.toString(), o.o2.toString());
		  if(o.o2.toString().equals("%rax")){
    		  asm.setg("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o2.toString().equals("%rdi")){
    		  asm.setg("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o2.toString().equals("%rsi")){
    		  asm.setg("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o2.toString().equals("%rdx")){
    		  asm.setg("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o2.toString().equals("%rcx")){
    		  asm.setg("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o2.toString().equals("%rsp")){
    		  asm.setg("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o2.toString().equals("%rbx")){
    		  asm.setg("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o2.toString().equals("%rbp")){
    		  asm.setg("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o2.toString().equals("%r8")){
    		  asm.setg("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o2.toString().equals("%r9")){
    		  asm.setg("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o2.toString().equals("%r10")) {
    		  asm.setg("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o2.toString().equals("%r11")){
    		  asm.setg("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o2.toString().equals("%r12")){
    		  asm.setg("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o2.toString().equals("%r13")){
    		  asm.setg("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o2.toString().equals("%r14")) {
    		  asm.setg("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o2.toString().equals("%r15")){
    		  asm.setg("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o2 instanceof Spilled){
    		  asm.setg(o.o2.toString());
    	  }
      }
      else if(o.m == Mbinop.Msetne) {
    	  asm.cmpq(o.o1.toString(), o.o2.toString());
    	  if(o.o2.toString().equals("%rax")){
    		  asm.setne("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o2.toString().equals("%rdi")){
    		  asm.setne("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o2.toString().equals("%rsi")){
    		  asm.setne("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o2.toString().equals("%rdx")){
    		  asm.setne("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o2.toString().equals("%rcx")){
    		  asm.setne("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o2.toString().equals("%rsp")){
    		  asm.setne("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o2.toString().equals("%rbx")){
    		  asm.setne("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o2.toString().equals("%rbp")){
    		  asm.setne("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o2.toString().equals("%r8")){
    		  asm.setne("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o2.toString().equals("%r9")){
    		  asm.setne("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o2.toString().equals("%r10")) {
    		  asm.setne("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o2.toString().equals("%r11")){
    		  asm.setne("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o2.toString().equals("%r12")){
    		  asm.setne("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o2.toString().equals("%r13")){
    		  asm.setne("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o2.toString().equals("%r14")) {
    		  asm.setne("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o2.toString().equals("%r15")){
    		  asm.setne("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o2 instanceof Spilled){
    		  asm.setne(o.o2.toString());
    	  }
      }
      else if(o.m == Mbinop.Msetge) {
    	  asm.cmpq(o.o1.toString(), o.o2.toString());
    	  if(o.o2.toString().equals("%rax")){
    		  asm.setge("%al");
    		  asm.movzbq("%al", "%rax");
    	  }
		  else if(o.o2.toString().equals("%rdi")){
    		  asm.setge("%dil");
    		  asm.movzbq("%dil", "%rdi");
    	  }
		  else if(o.o2.toString().equals("%rsi")){
    		  asm.setge("%sil");
    		  asm.movzbq("%sil", "%rsi");
    	  }
		  else if(o.o2.toString().equals("%rdx")){
    		  asm.setge("%dl");
    		  asm.movzbq("%dl", "%rdx");
    	  }
		  else if(o.o2.toString().equals("%rcx")){
    		  asm.setge("%cl");
    		  asm.movzbq("%cl", "%rcx");
    	  }
		  else if(o.o2.toString().equals("%rsp")){
    		  asm.setge("%spl");
    		  asm.movzbq("%spl", "%rsp");
    	  }
		  else if(o.o2.toString().equals("%rbx")){
    		  asm.setge("%bl");
    		  asm.movzbq("%bl", "%rbx");
    	  }
		  else if(o.o2.toString().equals("%rbp")){
    		  asm.setge("%bpl");
    		  asm.movzbq("%bpl", "%rbp");
    	  }
		  else if(o.o2.toString().equals("%r8")){
    		  asm.setge("%r8b");
    		  asm.movzbq("%r8b", "%r8");
    	  }
    	  else if(o.o2.toString().equals("%r9")){
    		  asm.setge("%r9b");
    		  asm.movzbq("%r9b", "%r9");
    	  }
		  else if(o.o2.toString().equals("%r10")) {
    		  asm.setge("%r10b");
    		  asm.movzbq("%r10b", "%r10");
    	  }
    	  else if(o.o2.toString().equals("%r11")){
    		  asm.setge("%r11b");
    		  asm.movzbq("%r11b", "%r11");
    	  }
    	  else if(o.o2.toString().equals("%r12")){
    		  asm.setge("%r12b");
    		  asm.movzbq("%r12b", "%r12");
    	  }
    	  else if(o.o2.toString().equals("%r13")){
    		  asm.setge("%r13b");
    		  asm.movzbq("%r13b", "%r13");
    	  }
		  else if(o.o2.toString().equals("%r14")) {
    		  asm.setge("%r14b");
    		  asm.movzbq("%r14b", "%r14");
    	  }
    	  else if(o.o2.toString().equals("%r15")){
    		  asm.setge("%r15b");
    		  asm.movzbq("%r15b", "%r15");
    	  }
    	  else if(o.o2 instanceof Spilled){
    		  asm.setge(o.o2.toString());
    	  }
      }
      
      lin(o.l);

      //if(debug) System.out.println("Lmbinop Fin");
  }
  
  public void visit(Lpush o){
	  //if(debug) System.out.println("Lpush Deb");
	  asm.pushq(o.o.toString()); 
	  lin(o.l);
	  //if(debug) System.out.println("Lpush Fin");
  }
  
  public void visit(Lpop o){
	  //if(debug) System.out.println("Lpop Deb");
	  asm.popq(o.r.name); 
	  lin(o.l);
	  //if(debug) System.out.println("Lpop Fin");
  }
  
  public void visit(Lcall o){
	  //if(debug) System.out.println("Lcall Deb");
	  asm.call(o.s); 
	  lin(o.l);
	  //if(debug) System.out.println("Lcall Fin");
  }
  
  public void visit(LTLfun f){
	  //if(debug) System.out.println("LTLfun Deb");
	  cfg = f.body;
	  asm.label(f.name);
	  lin(f.entry);
	  //if(debug) System.out.println("LTLfun Fin");
  }
  public void visit(LTLfile o){
	  //if(debug) System.out.println("LTLfile Deb");
	  asm = new X86_64(); 
	  visited = new HashSet<>();
	  asm.globl("main"); 
	  for(LTLfun f: o.funs)
        this.visit(f);
	  //if(debug) System.out.println("LTLfile Fin");
  }
  
  public void write(String f){
      this.asm.printToFile(f);
  }
  
}
