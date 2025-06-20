package actions;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;

import actions.views.EmployeeView;
import constants.AttributeConst;
import constants.ForwardConst;
import constants.JpaConst;
import services.EmployeeService;

/**
 * 従業員に関わる処理を行うActionクラス
 */
public class EmployeeAction extends ActionBase {

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
     * 一覧画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void index() throws ServletException, IOException {

        // 指定されたページ数の一覧画面に表示するデータを取得
        int page = getPage();
        List<EmployeeView> employees = service.getPerPage(page);

        // 全ての従業員データの件数を取得
        long employeeCount = service.countAll();

        putRequestScope(AttributeConst.EMPLOYEES, employees); // 取得した従業員データ
        putRequestScope(AttributeConst.EMP_COUNT, employeeCount); // 全ての従業員データの件数
        putRequestScope(AttributeConst.PAGE, page); // ページ数
        putRequestScope(AttributeConst.MAX_ROW, JpaConst.ROW_PER_PAGE); // 1ページに表示するレコードの数

        // セッションにフラッシュメッセージが設定されている場合はリクエストスコープに移し替え、セッションからは削除する
        String flush = getSessionScope(AttributeConst.FLUSH);
        if (flush != null) {
            putRequestScope(AttributeConst.FLUSH, flush);
            removeSessionScope(AttributeConst.FLUSH);
        }

        // 一覧画面を表示
        forward(ForwardConst.FW_EMP_INDEX);
    }
}
/**
 * ログイン中の従業員が管理者かどうかチェックし、管理者でない場合はエラー画面を表示
 * @return true: 管理者 false: 管理者ではない
 * @throws ServletException
 * @throws IOException
 */
private boolean checkAdmin() throws ServletException, IOException {

    // セッションからログイン中の従業員情報を取得
    EmployeeView ev = (EmployeeView) getSessionScope(AttributeConst.LOGIN_EMP);

    // 管理者でなければエラー画面を表示
    if (ev.getAdminFlag() != AttributeConst.ROLE_ADMIN.getIntegerValue()) {

        forward(ForwardConst.FW_ERR_UNKNOWN);
        return false;
    } else {

        return true;
    }
}
/**
 * 登録処理を行う
 * @throws ServletException
 * @throws IOException
 */
public void create() throws ServletException, IOException {

    // CSRF対策 tokenのチェック
    if (checkToken()) {

        // パラメータの値を元に従業員情報のインスタンスを作成する
        EmployeeView ev = new EmployeeView(
                null,
                getRequestParam(AttributeConst.EMP_CODE),
                getRequestParam(AttributeConst.EMP_NAME),
                getRequestParam(AttributeConst.EMP_PASS),
                Integer.parseInt(getRequestParam(AttributeConst.EMP_ADMIN_FLG)),
                null,
                null,
                AttributeConst.DEL_FLAG_FALSE.getIntegerValue());

        // アプリケーションスコープからpepper文字列を取得
        String pepper = getContextScope(PropertyConst.PEPPER);

        // 従業員情報登録
        List<String> errors = service.create(ev, pepper);

        if (errors.size() > 0) {
            // 登録中にエラーがあった場合

            putRequestScope(AttributeConst.TOKEN, getTokenId()); // CSRF対策用トークン
            putRequestScope(AttributeConst.EMPLOYEE, ev); // 入力された従業員情報
            putRequestScope(AttributeConst.ERR, errors); // エラーのリスト

            // 新規登録画面を再表示
            forward(ForwardConst.FW_EMP_NEW);
        } else {
            // 登録中にエラーがなかった場合

            // セッションに登録完了のフラッシュメッセージを設定
            putSessionScope(AttributeConst.FLUSH, MessageConst.I_REGISTERED.getMessage());

            // 一覧画面にリダイレクト
            redirect(ForwardConst.ACT_EMP, ForwardConst.CMD_INDEX);
        }
    }
}