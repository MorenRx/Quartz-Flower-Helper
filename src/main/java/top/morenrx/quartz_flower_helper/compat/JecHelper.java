package top.morenrx.quartz_flower_helper.compat;

import me.towdium.jecharacters.utils.Match;

public class JecHelper {
    public static boolean contains(String target, String query) {
        return Match.contains(target, query);
    }
}
