/*Copyright ©2024 APIJSON(https://github.com/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.milvus;

import apijson.JSONResponse;
import apijson.NotNull;
import apijson.RequestMethod;
import apijson.StringUtil;
import apijson.orm.AbstractParser;
import apijson.orm.SQLConfig;
import com.alibaba.fastjson.JSONObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.MutationResult;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import org.datayoo.moql.RecordSet;
import org.datayoo.moql.querier.milvus.MilvusQuerier;

import java.math.BigDecimal;
import java.util.*;

import static apijson.orm.AbstractSQLExecutor.KEY_RAW_LIST;


/**
 * @author Lemon
 * @see DemoSQLExecutor 重写 execute 方法：
 *     \@Override
 *      public JSONObject execute(@NotNull SQLConfig<Long> config, boolean unknownType) throws Exception {
 *          if (config.isMilvus()) {
 *              return MilvusUtil.execute(config, null, unknownType);
 *          }
 *
 *          return super.execute(config, unknownType);
 *     }
 */
public class MilvusUtil {
    public static final String TAG = "MilvusUtil";

    public static final Map<String, MilvusServiceClient> CLIENT_MAP = new LinkedHashMap<>();
    public static <T> MilvusServiceClient getClient(@NotNull SQLConfig<T> config) {
        String uri = config.getDBUri();
        String key = uri + (uri.contains("?") ? "&" : "?") + "username=" + config.getDBAccount();

        MilvusServiceClient conn = CLIENT_MAP.get(key);
        if (conn == null) {
            conn = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withUri(config.getDBUri())
                            .withAuthorization(config.getDBAccount(), config.getDBPassword())
                            .build()
            );
            CLIENT_MAP.put(key, conn);
        }
        return conn;
    }

    public static <T> void closeClient(@NotNull SQLConfig<T> config) {
        MilvusServiceClient conn = getClient(config);
        if (conn != null) {
            String uri = config.getDBUri();
            String key = uri + (uri.contains("?") ? "&" : "?") + "username=" + config.getDBAccount();
            CLIENT_MAP.remove(key);

//            try {
                conn.close();
//            }
//            catch (Throwable e) {
//                e.printStackTrace();
//            }
        }
    }

    public static <T> void closeAllClient() {
        Collection<MilvusServiceClient> cs = CLIENT_MAP.values();
        for (MilvusServiceClient c : cs) {
            try {
                c.close();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
        
        CLIENT_MAP.clear();
    }


    public static <T> JSONObject execute(@NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        if (RequestMethod.isQueryMethod(config.getMethod())) {
            return execQuery(config, sql, unknownType);
        }

        return executeUpdate(config, sql);
    }

    public static <T> int execUpdate(SQLConfig<T> config, String sql) throws Exception {
        JSONObject result = executeUpdate(config, sql);
        return result.getIntValue(JSONResponse.KEY_COUNT);
    }

    public static <T> JSONObject executeUpdate(SQLConfig<T> config, String sql) throws Exception {
        return executeUpdate(null, config, sql);
    }
    public static <T> JSONObject executeUpdate(MilvusServiceClient client, SQLConfig<T> config, String sql) throws Exception {
        if (client == null) {
            client = getClient(config);
        }

        R<MutationResult> mr;
        JSONObject result = AbstractParser.newSuccessResult();

        RequestMethod method = config.getMethod();
        if (method == RequestMethod.POST) {
            List<String> cl = config.getColumn();
            List<List<Object>> vs = config.getValues();

            List<InsertParam.Field> fl = new ArrayList<>(cl == null ? 0 : cl.size());
            if (cl != null) {
                Map<String, List<Object>> map = new HashMap<>();
                for (int i = 0; i < vs.size(); i++) {
                    List<Object> vl = vs.get(i);
                    for (int j = 0; j < cl.size(); j++) {
                        String k = cl.get(j);
                        List<Object> nvl = map.get(k);
                        if (nvl == null) {
                            nvl = new ArrayList<>();
                            map.put(k, nvl);
                        }

                        Object v = vl.get(j);
                        if (v instanceof BigDecimal) {
                            v = ((BigDecimal) v).floatValue();
                        }
                        else if (v instanceof Collection) {
                            Collection c = (Collection) v;
                            ArrayList<Object> nl = new ArrayList<>();
                            for (Object cv : c) {
                                if (cv instanceof BigDecimal) {
                                    nl.add(((BigDecimal) cv).floatValue());
                                }
                                else {
                                    nl.add(cv);
                                }
                            }

                            v = nl;
                        }

                        nvl.add(v);
                    }
                }

                Set<Map.Entry<String, List<Object>>> set = map.entrySet();
                for (Map.Entry<String, List<Object>> ety : set) {
                    InsertParam.Field f = new InsertParam.Field(ety.getKey(), ety.getValue());
                    fl.add(f);
                }
            }

            InsertParam param = InsertParam.newBuilder()
                    .withCollectionName(config.getSQLTable())
                    .withFields(fl)
                    .build();

            mr = client.insert(param);
        }
        else if (method == RequestMethod.PUT) {
//            UpdateCredentialParam param = UpdateCredentialParam.newBuilder()
//                    .build();
//            milvusClient.updateCredential(param);
            throw new UnsupportedOperationException("Milvus Java SDK 暂不支持修改记录！");
        }
        else if (method == RequestMethod.DELETE) {
            DeleteParam param = DeleteParam.newBuilder()
                    .withCollectionName(config.getSQLTable())
                    .withExpr(config.setPrepared(false).getWhereString(false))
                    .build();
            mr = client.delete(param);
        }
        else {
            throw new UnsupportedOperationException("Milvus Java SDK 暂不支持 APIJSON " + method + " 这个操作！");
        }

        if (mr == null) {
            return result;
        }

        if (mr.getException() != null) {
            throw mr.getException();
        }

        MutationResult data = mr.getData();
        List<Integer> sl = data.getSuccIndexList();
        int sc = sl == null ? 0 : sl.size();

        result.put(JSONResponse.KEY_COUNT, sc);
        if (config.getId() != null) {
            result.put(JSONResponse.KEY_ID, config.getId());
        }

        List<Integer> el = data.getErrIndexList();
        int fc = el == null ? 0 : el.size(); // data.getInsertCnt() - data.getSuccIndexCount();
        if (fc > 0) {
            result.put("failCount", fc);
            result.put("failIdList", el);

            result.put("successCount", sc);
            result.put("successIdList", sl);
        }

        return result;
    }


    public static <T> JSONObject execQuery(@NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        List<JSONObject> list = executeQuery(config, sql, unknownType);
        JSONObject result = list == null || list.isEmpty() ? null : list.get(0);
        if (result == null) {
            result = new JSONObject(true);
        }

        if (list != null && list.size() > 1) {
            result.put(KEY_RAW_LIST, list);
        }

        return result;
    }

    public static <T> List<JSONObject> executeQuery(@NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        return executeQuery(null, config, sql, unknownType);
    }
    public static <T> List<JSONObject> executeQuery(MilvusServiceClient client, @NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        if (client == null) {
            client = getClient(config);
        }

        /*
            查询语句含义：从book集合中筛选数据，并返回col1,col2两个列。筛选条件为，当数据的col3列值为4，col4列值为'a','b','c'中的任意一
            个，且vec向量字段采用'L2'类型匹配，值为'[[1.0, 2.0, 3.0],[1.1,2.1,3.1]]'。另外，采用强一致性级别在10个单元内进行检索，取第11到第15，5条命中记录。
        */
//      String sql = "select id,userId,momentId,content,date from Comment where vMatch(vec, 'L2', '[[1]]') and consistencyLevel('STRONG')  limit 1,1";

        // 使用Milvus客户端创建Milvus查询器
        MilvusQuerier milvusQuerier = new MilvusQuerier(client);
        // 使用查询器执行sql语句，并返回查询结果
        RecordSet recordSet = milvusQuerier.query(StringUtil.isEmpty(sql) ? config.getSQL(false) : sql);

//      int count = recordSet == null ? 0 : recordSet.getRecordsCount();
        List<Map<String, Object>> list = recordSet == null ? null : recordSet.getRecordsAsMaps();
//      RecordSetDefinition def = recordSet.getRecordSetDefinition();
//      List<ColumnDefinition> cols = def.getColumns();

//      List<Object[]> list = count <= 0 ? null : recordSet.getRecords();

        if (list == null) {
            return null;
        }

        List<JSONObject> nl = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);

            JSONObject obj = map == null ? new JSONObject(1) : new JSONObject(new LinkedHashMap<>(map));
            // obj.put(col.getValue(), os[j]);
//          for (int j = 0; j < os.length; j++) {
//              ColumnDefinition col = cols.get(j);
//              obj.put(col.getValue(), os[j]);
//          }
            nl.add(obj);
        }

        return nl;
    }

}
