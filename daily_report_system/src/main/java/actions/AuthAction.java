package actions;

import java.io.IOException;
import javax.servlet.ServletException;

import constants.AttributeConst;
import constants.ForwardConst;
import services.EmployeeService;

import actions.views.EmployeeView; 
import constants.MessageConst;
import constants.PropertyConst;
/**
 * 認証に関する処理を行うActionクラス
 */
public class AuthAction extends ActionBase {

    private EmployeeService service;

    /**
     * メソッドを実行する
     */
    @Override
    public void process() throws ServletException, IOException {

        service = new EmployeeService();

        // メソッドを実行
        invoke();

        service.close();
    }
    /**
     * ログイン処理を行う
     * @throws ServletException
     * @throws IOException
     */
    public void login() throws ServletException, IOException {

        String code = getRequestParam(AttributeConst.EMP_CODE);
        String plainPass = getRequestParam(AttributeConst.EMP_PASS);
        String pepper = getContextScope(PropertyConst.PEPPER);

        // 有効な従業員か認証する
        Boolean isValidEmployee = service.validateLogin(code, plainPass, pepper);

        if (isValidEmployee) {
            // 認証成功の場合

            // CSRF対策 tokenのチェック
            if (checkToken()) {

                // ログインした従業員情報を取得
                EmployeeView ev = service.findOne(code, plainPass, pepper);
                // セッションにログインした従業員情報を設定
                putSessionScope(AttributeConst.LOGIN_EMP, ev);
                // セッションにログイン完了のフラッシュメッセージを設定
                putSessionScope(AttributeConst.FLUSH, MessageConst.I_LOGGEDIN.getMessage());
                // トップページへリダイレクト
                redirect(ForwardConst.ACT_TOP, ForwardConst.CMD_INDEX);
            }
        } else {
            // 認証失敗の場合

            // CSRF対策用トークンを設定
            putRequestScope(AttributeConst.TOKEN, getTokenId());
            // 認証失敗のエラーメッセージ表示フラグを設定
            putRequestScope(AttributeConst.LOGIN_ERR, true);
            // 入力された社員番号を設定
            putRequestScope(AttributeConst.EMP_CODE, code);

            // ログイン画面を表示
            forward(ForwardConst.FW_LOGIN);
        }
    }
    /**
     * ログイン画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void showLogin() throws ServletException, IOException {

        // CSRF対策用トークンを設定
        putRequestScope(AttributeConst.TOKEN, getTokenId());

        // セッションにフラッシュメッセージが設定されている場合はリクエストスコープに設定する
        String flush = getSessionScope(AttributeConst.FLUSH);
        if (flush != null) {
            putRequestScope(AttributeConst.FLUSH, flush);
            removeSessionScope(AttributeConst.FLUSH);
        }

        // ログイン画面を表示
        forward(ForwardConst.FW_LOGIN);
    }
}