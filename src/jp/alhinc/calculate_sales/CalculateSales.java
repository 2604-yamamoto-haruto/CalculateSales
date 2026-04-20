package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "ファイルのフォーマットが不正です";
	private static final String FILE_NOT_CONESCUTIVE_NUMBERS = "売上ファイル名が連番になっていません";
	private static final String TOTAL_AMOUNT_EXCEEDED = "売上金額が10桁を超えました";
	private static final String INVALID_STORE_CODE = "コードが不正です";

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
		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();
		// 支店別集計用正規表現
		String branchRegex = "^[0-9]{3}$";
		// 商品別集計用正規表現
		String commodityRegex = "^[A-Za-z0-9]{8}$";
		// 支店
		String branch = "支店";
		// 商品
		String commodity = "商品";
		if (args.length != 1) {
		   System.out.println(UNKNOWN_ERROR);
		   return;
		}

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, branchRegex, branch)) {
			return;
		}
		// 商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales, commodityRegex, commodity)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		BufferedReader br = null;
		List<File> rcdFiles = new ArrayList<>();
		File[] files = new File(args[0]).listFiles();
		Long saleAmount;
		Long commodityAmount;

		for(int i = 0; i < files.length; i++) {
			if(files[i].isFile() && files[i].getName().matches("^[0-9]{8}[.]rcd$")){
				rcdFiles.add(files[i]);
			}
		}
		Collections.sort(rcdFiles);
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
				if(codeSales.size() != 3) {
				    System.out.println(rcdFiles.get(0) + FILE_INVALID_FORMAT);
				    return;
				}
				if (!branchNames.containsKey(codeSales.get(0))) {
				    System.out.println(rcdFiles.get(0) + branch + INVALID_STORE_CODE);
					return;
				}
				if (!commodityNames.containsKey(codeSales.get(1))) {
				    System.out.println(rcdFiles.get(0) + commodity + INVALID_STORE_CODE);
					return;
				}
				if(!codeSales.get(2).matches("^[0-9]*$")) {
				    System.out.println(UNKNOWN_ERROR);
				    return;
				}
				long fileSale = Long.parseLong(codeSales.get(2));
				saleAmount = branchSales.get(codeSales.get(0)) + fileSale;

				long productSales = Long.parseLong(codeSales.get(2));
				commodityAmount = commoditySales.get(codeSales.get(1)) + productSales;

				if(saleAmount >= 10000000000L || commodityAmount >= 10000000000L){
					System.out.println(TOTAL_AMOUNT_EXCEEDED);
					return;
				}

				branchSales.put(codeSales.get(0), saleAmount);
				commoditySales.put(codeSales.get(1), commodityAmount);
			}catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
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
		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
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
	private static boolean readFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales, String regex, String item) {
		BufferedReader br = null;
		Long num = 0L;

		try {
			File file = new File(path, fileName);
			if(!file.exists()) {
				System.out.println(item + FILE_NOT_EXIST);
			    return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");
				if((items.length != 2) || (!items[0].matches(regex))){
					System.out.println(item + FILE_INVALID_FORMAT);
				    return false;
				}
				names.put(items[0], items[1]);
				sales.put(items[0], num);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
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
	private static boolean writeFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		for (String key : names.keySet()) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))){
				bw.write(key + "," + names.get(key) + "," + sales.get(key));
				bw.newLine();
			}catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return false;
			}
		}
		return true;
	}

}