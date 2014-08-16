package org.easyframe.tutorial.lessona;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.common.wrapper.Page;
import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.NativeQuery;
import jef.database.ORMConfig;
import jef.database.QB;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.RoutingDataSource;
import jef.database.datasource.SimpleDataSource;
import jef.database.query.Query;
import jef.database.query.Selects;
import jef.database.query.SqlExpression;
import jef.tools.DateUtils;
import jef.tools.string.RandomData;

import org.easyframe.tutorial.lessona.entity.Customer;
import org.easyframe.tutorial.lessona.entity.Device;
import org.easyframe.tutorial.lessona.entity.OperateLog;
import org.easyframe.tutorial.lessona.entity.Person2;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * 分库分表后的对象操作演示（基本）
 * 
 * @author jiyi
 * 
 */
public class Case2 extends org.junit.Assert {
	private static DbClient db;
	private static boolean doinit = true;

	/**
	 * 准备测试数据
	 * 
	 * @throws SQLException
	 */
	@BeforeClass
	public static void setup() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial.lessona");
		// 准备多个数据源
		Map<String, DataSource> datasources = new HashMap<String, DataSource>();

		// 创建三个数据库。。。
		datasources.put("datasource1", new SimpleDataSource("jdbc:derby:./db;create=true", null, null));
		datasources.put("datasource2", new SimpleDataSource("jdbc:derby:./db2;create=true", null, null));
		datasources.put("datasource3", new SimpleDataSource("jdbc:derby:./db3;create=true", null, null));
		MapDataSourceLookup lookup = new MapDataSourceLookup(datasources);
		lookup.setDefaultKey("datasource1");// 指定datasource1是默认的操作数据源

		// 构造一个带数据路由功能的DbClient
		db = new DbClient(new RoutingDataSource(lookup));
		if (doinit) {
			db.dropTable(Customer.class, Device.class, OperateLog.class, Person2.class);

			// System.err.println("现在开始为Customer对象建表。Customer对象按“年_月”方式分表，并且按customerNo除以3的余数分库");
			// db.createTable(Customer.class);
			// System.out.println();

			System.err.println("现在开始为Device对象建表。Device对象按“IndexCode的头两位数字”分表，当头两位数字介于10~20时分布于ds1；21~32时分布于ds2；33~76时分布于ds3；其他情形时分布于默认数据源");
			db.createTable(Device.class);
			System.out.println();
			//
			// System.err.println("现在开始为OperateLog对象建表。OperateLog对象按“年_月_日”方式分表，不分库");
			// db.createTable(OperateLog.class);
			// System.out.println();
			//
			// System.err.println("现在开始为Person2对象建表。Person2对象是垂直拆分，因此所有数据都位于datasource2上。不分表");
			// db.createTable(Person2.class);
			// System.out.println();

			System.err.println("======= 建表操作完成，对于分区表只创建了可以预测到的若干表，实际操作中需要用到的表会自动按需创建=========");
		}
	}

	/**
	 * 当分表结果计算后，发现没有需要查询的表的时候，会直接返回
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testNoMatchTables() throws SQLException {
		Query<Device> d = QB.create(Device.class);
		d.addCondition(Device.Field.indexcode, "9999999");
		db.select(d);

		db.select(d);
	}

	/**
	 * 当采用垂直拆分时， Person2的所有操作都集中在DB2上。
	 * 
	 * @throws SQLException
	 */
	@Test
	public void test1() throws SQLException {
		// 插入
		Person2 p2 = new Person2();
		p2.setName("Jhon smith");
		db.insert(p2);

		// 查询
		Person2 loaded = db.load(Person2.class, p2.getId());

		// 更新
		loaded.setName("Kingstone");
		assertEquals(1, db.update(loaded));

		// 查询数量
		assertEquals(1, db.count(QB.create(Person2.class)));

		// 删除
		assertEquals(1, db.delete(loaded));
		// 删除后
		assertEquals(0, db.count(QB.create(Person2.class)));
	}

	/**
	 * Customer对象按创建记录created字段时的“年_月”方式分表，并且按customerNo除以3的余数分库
	 * 
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCustomer() throws SQLException {
		// 插入
		Customer c = new Customer();
		c.setLastName("Hession");
		c.setFirstName("Obama");
		c.setDOB(DateUtils.getDate(1904, 3, 4));
		c.setDOD(DateUtils.getDate(1991, 7, 12));
		c.setEmail("obama@hotmail.com");
		db.insert(c);
		/*
		 * 这个案例中，用作分库分表的customerNo和created这两个字段都没赋值，但是记录却成功的插入了。
		 * 因为customerNo是自增值，created是系统自动维护的当前时间值。系统自动维护了这两个字段，从而使得分库分表的路由条件充分了。
		 * 此外，注意自增值的生成方式变化了。在本例中，使用TABLE来实现自增值。因为分库分表后，表的自增值已经无法保证唯一了,
		 * 而Derby不支持Sequence，所以采用TABLE方式生成自增值。(DERBY实际上支持，但兼容性有些问题，故关闭了)
		 */

		System.out.println("====将会按需建表====");
		Customer c2 = new Customer();
		c2.setLastName("Joe");
		c2.setFirstName("Lei");
		c2.setDOB(DateUtils.getDate(1981, 3, 4));
		c2.setEmail("joe@hotmail.com");
		c2.setCreateDate(DateUtils.getDate(2013, 5, 1));
		db.insert(c2);
		/*
		 * 这里指定记录创建时间为2013-5-1，也就是说该条记录需要插到表 CUSTOMER_201305这张表中。
		 * 这张表虽然现在不存在，但是EF-ORM会在需要用到时，自动创建这张表。
		 */

		// 查询。
		Customer loaded = db.load(Customer.class, c.getCustomerNo());

		// 更新
		loaded.setLastName("King");
		assertEquals(1, db.update(loaded));

		// 查询数量
		assertEquals(1, db.count(QB.create(Customer.class)));

		// 删除
		assertEquals(1, db.delete(loaded));
		// 删除后
		assertEquals(0, db.count(QB.create(Customer.class)));
	}

	/**
	 * 演示Batch操作在分库分表后发生的变化
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testCustomerBatch() throws SQLException {
		List<Customer> list = new ArrayList<Customer>();
		// 批量插入
		Customer c = new Customer();
		c.setLastName("Hession");
		c.setFirstName("Obama");
		c.setDOB(DateUtils.getDate(1904, 3, 4));
		c.setDOD(DateUtils.getDate(1991, 7, 12));
		c.setEmail("obama@hotmail.com");
		list.add(c);

		c = new Customer();
		c.setLastName("Joe");
		c.setFirstName("Lei");
		c.setDOB(DateUtils.getDate(1981, 3, 4));
		c.setEmail("joe@hotmail.com");
		c.setCreateDate(DateUtils.getDate(2013, 5, 1));
		list.add(c);

		c = new Customer();
		c.setCustomerNo(4);
		c.setLastName("Yang");
		c.setFirstName("Fei");
		c.setDOB(DateUtils.getDate(1976, 12, 15));
		c.setEmail("fei@hotmail.com");
		c.setCreateDate(DateUtils.getDate(2013, 5, 1));
		list.add(c);

		db.batchInsert(list);

		// 跨表跨库的搜索
		Query<Customer> query = QB.create(Customer.class);
		query.addCondition(QB.between(Customer.Field.createDate, DateUtils.getDate(2013, 4, 30), DateUtils.getDate(2014, 12, 31)));

		// 艰难的查找——
		// 由于条件只有一个时间范围，因此会搜索所有可能出现记录的表。包括——三个数据库上所有时段在13年4月到14年12月的表。理论上所有可能的组合。
		// 但由于大部分表不存在。EF-ORM只会查找实际存在的表，实际上不存在的表不会被查找。
		list = db.select(query);
		assertEquals(3, list.size());

		// 批量更新
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setEmail("mail" + i + "@hotmail.com");
		}
		// 虽然是Batch更新，但实际上所有记录分散在不同的库的不同的表中，重新分组后，每张表只有一条记录。
		db.batchUpdate(list);

		// 批量删除
		db.batchDelete(list);
	}

	@Test
	public void testDeviceSelect() throws SQLException {
		List<Device> list = generateDevice(50);
		ORMConfig.getInstance().setMaxBatchLog(2);
		db.batchInsert(list);
		System.err.println("=====插入50条记录完成=====");
		System.out.println("当前总数是:" + db.count(QB.create(Device.class)));
		{
			Query<Device> query = QB.create(Device.class);
			query.addCondition(QB.matchStart(Device.Field.indexcode, "4"));

			List<Device> results = db.select(query);
			System.out.println("=====查询indexCode 4开头的记录完成=====");
			LogUtil.show(results);
		}
		{
			System.out.println("=====查询indexcode含0的记录，并按创建日期排序=====");
			// 分库分表后的难点之一——跨库查询并且并且排序
			Query<Device> query = QB.create(Device.class);
			query.addCondition(QB.matchAny(Device.Field.indexcode, "0"));// 显然这个查询意味着要调动好几个数据库上的好几张表
			query.orderByDesc(Device.Field.createDate); // 更麻烦的是，要对这些数据按日期进行排序。
			List<Device> results = db.select(query);
			for (Device ss : results) {
				System.out.println(ss);
			}

			System.out.println("=====查询indexcode含0的记录，并按创建日期排序，每页8条，显示第二页=====");
			// 更麻烦一点——[跨库查询]并且并且[排序]还要[分页]——每页10条，从第二页开始显示
			Page<Device> page = db.pageSelect(query, Device.class, 10).setOffset(10).getPageData();
			System.out.println("总数:" + page.getTotalCount() + " 每页:" + page.getPageSize());
			LogUtil.show(page.getList());
		}
		{

			System.out.println("=====跨数据库 聚合查询=====");
			// 分库分表后的难点之二——聚合查询
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type).group();
			select.column(Device.Field.indexcode).count().as("ct");
			List<String[]> strs = db.selectAs(query, String[].class);
			for (String[] ss : strs) {
				System.out.println(Arrays.toString(ss));
			}
		}
		{
			System.out.println("=====跨数据库 聚合+重新排序 查询=====");
			// 分库分表后的难点之二——聚合查询+再加上排序
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type).group();
			select.column(Device.Field.indexcode).count().as("ct");
			query.orderByDesc(new SqlExpression("ct")); // 除了聚合以外，再添加聚合后排序

			List<String[]> strs = db.selectAs(query, String[].class);
			for (String[] ss : strs) {
				System.out.println(Arrays.toString(ss));
			}
		}
		{

			System.out.println("=====跨数据库 Distinct 查询=====");
			// 分库分表后的难点之三——Distinct操作
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type);
			select.setDistinct(true);
			// query.orderByDesc(new SqlExpression("ct"));

			List<Device> results = db.select(query);
			for (Device ss : results) {
				System.out.println(ss.getName() + " " + ss.getType());
			}
			assertTrue(results.size() > 0);
			assertNotNull(results.get(0).getType());
		}
		{
			System.out.println("=====跨数据库查询并带排序分页=====");
			// 分库分表后的难点之三——Distinct操作
			Query<Device> query = QB.create(Device.class);
			query.orderByDesc(Device.Field.indexcode);
			List<Device> results = db.select(query, new IntRange(11, 20));
			for (Device ss : results) {
				System.out.println(ss);
			}
			assertTrue(results.size() > 0);
			assertNotNull(results.get(0).getType());
		}
		{
			System.out.println("=====跨数据库 聚合+Having +重新排序+分页=====");
			// 分库分表后的难点组合——groupBy Having 排序 分页一起上吧！
			Query<Device> query = QB.create(Device.class);
			Selects select = QB.selectFrom(query);
			select.column(Device.Field.type).group();
			select.column(Device.Field.indexcode).count().as("ct").having(Operator.LESS_EQUALS, 6);
			query.orderByDesc(new SqlExpression("ct")); // 除了聚合以外，再添加聚合后排序

			List<String[]> strs = db.selectAs(query, String[].class);
			for (String[] ss : strs) {
				System.out.println(Arrays.toString(ss));
			}

		}
	}

	@Test
	public void testNativeQuery() throws SQLException{
		List<Device> list = generateDevice(50);
		ORMConfig.getInstance().setMaxBatchLog(2);
		db.batchInsert(list);
		{//SQL语句增
			NativeQuery nq=db.createNativeQuery("insert into DeVice(indexcode,name,type,createDate) values(:code, :name, :type, sysdate)");
			nq.setRouting(true);
			nq.setParameter("code", "122346");
			nq.setParameter("name", "测试插入数据");
			nq.setParameter("type", "办公用品");
			nq.executeUpdate();
			
			nq.setParameter("code", "7822346");
			nq.setParameter("name", "非官方的得到");
			nq.setParameter("type", "大家电");
			nq.executeUpdate();
			
			nq.setParameter("code", "452346");
			nq.setParameter("name", "萨菲是方式飞");
			nq.setParameter("type", "日用品");
			nq.executeUpdate();			
		}
		{//SQL语句改
			
			NativeQuery nq=db.createNativeQuery("update DeVice xx set xx.name='ID:'||indexcode,\ncreateDate=sysdate where indexcode between :s1 and :s2");
			nq.setRouting(true);
			nq.setParameter("s1", "1000");
			nq.setParameter("s2", "6000");
			nq.executeUpdate();
		}
		//改
		
		
		
		
		
		//删
		
		//查
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		//问题1 不同的函数拆解后会产生重复列。同时 *会和其他所有列重复 (OK)
		//问题2 系统union后产生的表名t会和用户自己定义的表名重合  (似乎没什么问题，设计为固定重合)
		//问题3 如果系统自动union产生表名随机，但是由于select项目沿用内部的语句，因此关于表名的alias会不匹配(OK、固定重合)
		
		//问题4, 在slect项中使用 t.*时，其table的name是t，而不是alias.
		//注意SQL中 count(*)会统计所有行，而count(column）会跳过column值为null的行，两者并不总是等效
		String sql="select type,count(tx.indexcode) as count,max(indexcode) max_id from Device tx where indexcode like '4%' or indexcode like '1123%' or indexcode like '6%' group by type ";
		NativeQuery<Map> query=db.createNativeQuery(sql,Map.class);
		query.setRouting(true);
		List<Map> devices=query.getResultList();
		for (Map ss : devices) {
			System.out.println(ss);
		}
		
		System.out.println(query.getResultCount());
		System.out.println(query.getResultIterator());
		
		
	}
	
	/*
	 * 生成一些随机的数据
	 */
	private List<Device> generateDevice(int i) {
		List<Device> result = Arrays.asList(RandomData.newArrayInstance(Device.class, i));
		String[] types = { "耗材", "大家电", "办公用品", "日用品", "电脑配件", "图书" };
		for (Device device : result) {
			device.setIndexcode(String.valueOf(RandomData.randomInteger(100000, 990000)));
			device.setCreateDate(RandomData.randomDate(DateUtils.getDate(2000, 1, 1), DateUtils.getDate(2014, 12, 31)));
			device.setType(types[RandomData.randomInteger(0, 6)]);
		}
		return result;
	}
}
