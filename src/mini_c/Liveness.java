package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Liveness {

  Map<Label, LiveInfo> info;

  Liveness(ERTLgraph g) {
    info = new HashMap<>();
    LiveInfo res;
    ERTL current;
    for (Label l : g.graph.keySet()) {
      current = g.graph.get(l);
      res = new LiveInfo(current);
      res.defs = current.def();
      res.succ = current.succ();
      res.uses = current.use();
      info.put(l, res);
    }
    for (Label l : info.keySet()) {
      for (Label s : info.get(l).succ) {
        info.get(s).pred.add(l);
      }
    }

    HashSet<Label> ws = new HashSet<>();
    for (Label l : info.keySet()) ws.add(l);

    do {
      Label l = null;
      for (Label tmp : ws) {
        l = tmp;
        break;
      }
      ws.remove(l);
      Set<Register> ins = new HashSet<Register>();
      Set<Register> outs = new HashSet<Register>();

      //outs ...
      for (Label s : info.get(l).succ) {
        for (Register r : info.get(s).ins) outs.add(r);
      }
      info.get(l).outs = new HashSet<>();
      info.get(l).outs.addAll(outs);

      //ins ...
      for (Register r : info.get(l).uses) {
        ins.add(r);
      }

      for (Register r : info.get(l).outs) {
        if (!info.get(l).defs.contains(r)) ins.add(r);
      }

      if (!ins.equals(info.get(l).ins)) {
        info.get(l).ins = new HashSet<>();
        info.get(l).ins.addAll(ins);
        ws.add(l);
        for (Label tmp : info.get(l).pred) ws.add(tmp);
      }
    } while (!ws.isEmpty());
  }

  private void print(Set<Label> visited, Label l) {
    if (visited.contains(l)) return;
    visited.add(l);
    LiveInfo li = this.info.get(l);
    System.out.println("  " + String.format("%3s", l) + ": " + li);
    for (Label s : li.succ) print(visited, s);
  }

  void print(Label entry) {
    System.out.println("------------ LIVENESS -----------");
    print(new HashSet<Label>(), entry);
  }

  class LiveInfo {

    ERTL instr;
    Label[] succ; // successeurs
    Set<Label> pred; // prédécesseurs
    Set<Register> defs; // définitions
    Set<Register> uses; // utilisations
    Set<Register> ins; // variables vivantes en entrée
    Set<Register> outs; // variables vivantes en sortie

    LiveInfo(ERTL instr) {
      pred = new HashSet<>();
      defs = new HashSet<>();
      uses = new HashSet<>();
      ins = new HashSet<>();
      outs = new HashSet<>();
      this.instr = instr;
    }

    public String toString() {
      return (
        this.instr.toString() +
        " d=" +
        defs +
        " u=" +
        uses +
        " i=" +
        ins +
        " o=" +
        outs
      );
    }
  }
}
