package th.pd.common.android;

/**
 * a standard api to transit commands among controllers<br/>
 * <br/>
 * do not confuse:<br/>
 * &emsp; callee -> caller: use callback;<br/>
 * &emsp; caller -> callee: just invoke the method<br/>
 *
 * @author tanghao
 */
public interface OnActionCallback {

    public boolean onAction(int actionId, Object extra);
}