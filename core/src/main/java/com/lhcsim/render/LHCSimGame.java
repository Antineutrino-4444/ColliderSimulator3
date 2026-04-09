package com.lhcsim.render;

import com.lhcsim.app.LhcSimGame;

/**
 * Backward-compatible alias for {@link LhcSimGame}.
 * <p>
 * Existing code references this class; it delegates entirely to the
 * canonical {@code app.LhcSimGame} superclass.
 *
 * @deprecated Use {@link LhcSimGame} directly.
 */
@Deprecated
public class LHCSimGame extends LhcSimGame {
}
