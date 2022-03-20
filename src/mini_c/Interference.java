package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import mini_c.Liveness.LiveInfo;

class Arcs {

  Set<Register> prefs = new HashSet<>();
  Set<Register> intfs = new HashSet<>();
}

class Interference {

  Map<Register, Arcs> graph;

  void print() {
    System.out.println("--------interference--------");
    for (Register r : graph.keySet()) {
      Arcs a = graph.get(r);
      System.out.println("  " + r + " pref=" + a.prefs + " intf=" + a.intfs);
    }
  }

  Interference(Liveness lg) {
    graph = new HashMap<>();
    for (LiveInfo infos : lg.info.values()) {
      if (!infos.defs.isEmpty()) {
        for (Register v : infos.defs) {
          if (
            infos.instr instanceof ERmbinop &&
            ((ERmbinop) infos.instr).m == Mbinop.Mmov
          ) {
            ERmbinop instr = (ERmbinop) infos.instr;
            if (graph.containsKey(v)) {
              graph.get(v).prefs.add(instr.r1);
            } else {
              Arcs arc = new Arcs();
              arc.prefs.add(instr.r1);
              graph.put(v, arc);
            }

            if (graph.containsKey(instr.r1)) {
              graph.get(instr.r1).prefs.add(v);
            } else {
              Arcs arc = new Arcs();
              arc.prefs.add(v);
              graph.put(instr.r1, arc);
            }
          }
        }
      }
    }
    for (LiveInfo infos : lg.info.values()) {
      if (!infos.defs.isEmpty()) {
        for (Register v : infos.defs) {
          if (
            infos.instr instanceof ERmbinop &&
            ((ERmbinop) infos.instr).m == Mbinop.Mmov
          ) {
            ERmbinop instr = (ERmbinop) infos.instr;
            for (Register w_i : infos.outs) {
              if (w_i != instr.r1) {
                if (v == w_i) continue;
                if (graph.containsKey(v)) {
                  graph.get(v).intfs.add(w_i);
                } else {
                  Arcs arc = new Arcs();
                  arc.intfs.add(w_i);
                  graph.put(v, arc);
                }

                if (graph.containsKey(w_i)) {
                  graph.get(w_i).intfs.add(v);
                } else {
                  Arcs arc2 = new Arcs();
                  arc2.intfs.add(v);
                  graph.put(w_i, arc2);
                }
              }
            }
          } else {
            for (Register w_i : infos.outs) {
              if (v == w_i) continue;
              if (graph.containsKey(v)) {
                graph.get(v).intfs.add(w_i);
              } else {
                Arcs arc = new Arcs();
                arc.intfs.add(w_i);
                graph.put(v, arc);
              }

              if (graph.containsKey(w_i)) {
                graph.get(w_i).intfs.add(v);
              } else {
                Arcs arc2 = new Arcs();
                arc2.intfs.add(v);
                graph.put(w_i, arc2);
              }
            }
          }
        }
      }
    }
  }
}
