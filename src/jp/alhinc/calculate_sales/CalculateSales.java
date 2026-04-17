package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_CONESCUTIVE_NUMBERS = "売上ファイル名が連番になっていません";
	private static final String TOTAL_AMOUNT_EXCEEDED = "売上金額が10桁を超えました";
	private static final String INVALID_STORE_CODE = "の支店コードが不正です";
	private static final String INVALID_FORMAT = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		if (args.length != 1) {
		   System.out.println(UNKNOWN_ERROR);
		   return;
		}

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		BufferedReader br = null;
		List<File> rcdFiles = new ArrayList<>();
		File[] files = new File(args[0]).listFiles();
		Long saleAmount;

		for(int i = 0; i < files.length; i++) {
			if(files[i].getName().matches("[0-9]{8}[.]rcd$")){
				rcdFiles.add(files[i]);
			}
		}
		for(int i = 0; i < rcdFiles.size() -1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));
			if((latter - former) != 1) {
				System.out.println(FILE_NOT_CONESCUTIVE_NUMBERS);
				return;
			}
		}
		for(int i = 0; i < rcdFiles.size(); i++) {
			List<String> codeSales = new ArrayList<>();
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				String line;
				while((line = br.readLine()) != null){
					codeSales.add(line);
				}
				if(codeSales.size() != 2) {
				    System.out.println(codeSales.get(0) + INVALID_FORMAT);
				}
				if (!branchNames.containsKey(codeSales.get(0))) {
				    System.out.println(codeSales.get(0) + INVALID_STORE_CODE);
					return;
				}
				if(!codeSales.get(1).matches("^[0-9]*$")) {
				    System.out.println();
				    return;
				}
				long fileSale = Long.parseLong(codeSales.get(1));
				saleAmount = branchSales.get(codeSales.get(0)) + fileSale;
				if(saleAmount >= 10000000000L){
					System.out.println(TOTAL_AMOUNT_EXCEEDED);
					return;
				}

				branchSales.put(codeSales.get(0), saleAmount);
			}catch(IOException e) {
				System.out.println(FILE_NOT_EXIST);
				return;
			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;
		Long num = 0L;

		try {
			File file = new File(path, fileName);
			if(!file.exists()) {
			    System.out.println(FILE_NOT_EXIST);
			    return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");
				if((items.length != 2) || (!items[0].matches("[0-9]{3}"))){
				    System.out.println(FILE_INVALID_FORMAT);
				    return false;
				}
				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], num);
			}
		} catch(IOException e) {
			System.out.println(FILE_NOT_EXIST);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

		/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		for (String key : branchNames.keySet()) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return false;
			}
		}
		return true;
	}

}