package com.mandarin.bcu.util.unit;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import com.mandarin.bcu.io.Reader;
import com.mandarin.bcu.main.MainBCU;
import com.mandarin.bcu.util.Data;
import com.mandarin.bcu.util.anim.AnimC;
import com.mandarin.bcu.util.basis.Combo;
import com.mandarin.bcu.util.entity.data.CustomUnit;
import com.mandarin.bcu.util.entity.data.PCoin;
import com.mandarin.bcu.util.pack.Pack;
import com.mandarin.bcu.util.system.FixIndexList;
import com.mandarin.bcu.util.system.P;
import com.mandarin.bcu.util.system.files.AssetData;
import com.mandarin.bcu.util.system.files.VFile;

public class Unit extends Data implements Comparable<Unit> {

	public static void readData() throws IOException {
		VFile.get("./org/unit").list().forEach(p -> new Unit(p));
		Queue<String> qs = VFile.readLine("./org/data/unitlevel.csv");
		List<Unit> lu = Pack.def.us.ulist.getList();
		FixIndexList<UnitLevel> l = Pack.def.us.lvlist;
		for (Unit u : lu) {
			String[] strs = qs.poll().split(",");
			int[] lv = new int[20];
			for (int i = 0; i < 20; i++)
				lv[i] = Integer.parseInt(strs[i]);
			UnitLevel ul = new UnitLevel(lv);
			if (!l.contains(ul)) {
				ul.id = l.size();
				l.add(ul);
			}
			int ind = l.indexOf(ul);
			u.lv = l.get(ind);
			u.lv.units.add(u);
		}
		UnitLevel.def = l.get(2);
		qs = VFile.readLine("./org/data/unitbuy.csv");
		for (Unit u : lu) {
			String[] strs = qs.poll().split(",");
			u.rarity = Integer.parseInt(strs[13]);
			u.max = Integer.parseInt(strs[50]);
			u.maxp = Integer.parseInt(strs[51]);
			u.info.fillBuy(strs);
		}

		String [] priority = chooser();
		String [][] unitnames = getName(priority);

		for(int i = 0; i < lu.size();i++) {
			for (int j=0;j<lu.get(i).forms.length;j++) {
				int lang = 0;
				while(lang < 4) {
					lu.get(i).forms[j].name = findName(j,i,lu.get(i).forms.length,unitnames[lang]);

					if(lu.get(i).forms[j].name != null)
						break;
					lang++;
				}

				if(lu.get(i).forms[j].name == null) {
					lu.get(i).forms[j].name = "";
				}
			}
		}
	}

	protected static String findName(int form,int num,int formnum,String [] names) {
		String name = null;

		if(names.length>num) {
			String [] wait = names[num].split("\t");
			if(wait.length > formnum) {
				if(!wait[form+1].equals("")) {
					name = wait[form+1];
				}
			}
		}

		return name;
	}

	protected static String[][] getName(String [] priority) {
		String[][] result = new String[4][];

		for(int i = 0; i <priority.length;i++) {
			String shortPath = "./lang"+priority[i]+"UnitName.txt";
			String longPath = Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.mandarin.BCU";

			VFile.root.build(shortPath,AssetData.getAsset(new File(longPath+shortPath.substring(1))));
			Queue<String> qs = VFile.getFile(shortPath).getData().readLine();
			result[i] = qs.toArray(new String[qs.size()]);
		}

		return result;
	}

	protected static String[] chooser() {
		String language = Locale.getDefault().getLanguage();
		String [] priority;
		switch (language) {
			case "en":
				priority = new String[]{"/en/", "/jp/", "/zh/", "/kr/"};
				break;
			case "ja":
				priority = new String[]{"/jp/", "/en/", "/zh/", "/kr/"};
				break;
			case "th":
				priority = new String[]{"/zh/", "/jp/", "/en/", "/kr/"};
				break;
			case "ko":
				priority = new String[]{"/kr/", "/jp/", "/en/", "/zh/"};
				break;
			default:
				priority = new String[]{"/en/", "/jp/", "/zh/", "/kr/"};
				break;
		}

		return priority;
	}

	public final Pack pack;
	public final int id;
	public int rarity, max, maxp;
	public Form[] forms;
	public UnitLevel lv;
	public final UnitInfo info = new UnitInfo();

	public Unit(int ID, Unit old, Pack p, UnitLevel ul) {
		pack = p;
		id = ID;
		rarity = old.rarity;
		max = old.max;
		maxp = old.maxp;
		lv = ul;
		forms = new Form[old.forms.length];
		for (int i = 0; i < forms.length; i++)
			forms[i] = old.forms[i].copy(this);
	}

	public Unit(VFile<AssetData> p) {
		pack = Pack.def;
		id = Reader.parseIntN(p.getName());
		Pack.def.us.ulist.add(this);
		String str = "./org/unit/" + trio(id) + "/";
		Queue<String> qs = VFile.readLine(str + "unit" + trio(id) + ".csv");
		forms = new Form[p.countSubDire()];
		for (int i = 0; i < forms.length; i++)
			forms[i] = new Form(this, i, str + SUFX[i] + "/", qs.poll());
		if (MainBCU.preload)
			for (Form f : forms)
				f.anim.edi.check();
	}

	protected Unit(Pack p, DIYAnim da, CustomUnit cu) {
		pack = p;
		id = p.id * 1000 + p.us.ulist.nextInd();
		forms = new Form[] { new Form(this, 0, "new unit", da.getAnimC(), cu) };
		max = 50;
		maxp = 0;
		rarity = 4;
		lv = UnitLevel.def;
		lv.units.add(this);
	}

	protected Unit(Pack p, int ID) {
		pack = p;
		id = ID;
	}

	protected Unit(Pack p, Unit u) {
		pack = p;
		id = p.id * 1000 + p.us.ulist.nextInd();
		p.us.ulist.add(this);
		rarity = u.rarity;
		max = u.max;
		maxp = u.maxp;
		lv = u.lv;
		lv.units.add(u);
		forms = new Form[u.forms.length];
		for (int i = 0; i < forms.length; i++) {
			String str = AnimC.getAvailable(id + "-" + i);
			AnimC ac = new AnimC(str, u.forms[i].anim);
			DIYAnim da = new DIYAnim(str, ac);
			DIYAnim.map.put(str, da);
			CustomUnit cu = new CustomUnit();
			cu.importData(u.forms[i].du);
			forms[i] = new Form(this, i, str, ac, cu);
		}
	}

	public List<Combo> allCombo() {
		List<Combo> ans = new ArrayList<>();
		for (Combo[] cs : Combo.combos)
			for (Combo c : cs)
				for (int[] is : c.units)
					if (is[0] == id) {
						ans.add(c);
						break;
					}
		return ans;
	}

	@Override
	public int compareTo(Unit u) {
		return id > u.id ? 1 : id < u.id ? -1 : 0;
	}

	public int getPrefLv() {
		return max + (rarity < 2 ? maxp : 0);
	}

	public int[] getPrefLvs() {
		int[] ans = new int[6];
		if (forms.length >= 3) {
			PCoin pc = forms[2].getPCoin();
			if (pc != null)
				ans = pc.max.clone();
		}
		ans[0] = getPrefLv();
		return ans;
	}

	@Override
	public String toString() {
		String desp = "";
		if (desp != null && desp.length() > 0)
			return trio(id) + " " + desp;
		if (forms[0].name.length() > 0)
			return trio(id) + " " + forms[0].name;
		return trio(id);
	}

}

class UnitInfo {

	public int[][] evo;
	public int[] price = new int[10];
	public int type;

	protected void fillBuy(String[] strs) {
		for (int i = 0; i < 10; i++)
			price[i] = Integer.parseInt(strs[2 + i]);
		type = Integer.parseInt(strs[12]);
		int et = Integer.parseInt(strs[23]);
		if (et >= 15000 && et < 17000) {
			evo = new int[6][2];
			evo[0][0] = Integer.parseInt(strs[27]);
			for (int i = 0; i < 5; i++) {
				evo[i][0] = Integer.parseInt(strs[28 + i * 2]);
				evo[i][1] = Integer.parseInt(strs[29 + i * 2]);
			}
		}
	}

}