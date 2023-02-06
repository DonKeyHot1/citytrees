import React, {useState} from "react";
import {CtTree, ctTreeOf, deleteTree, isTreeDeletable, isTreeEditable, updateTree} from "../../components/Map/Models/CtTree";
import api from "../../api";
import {TreeStatus} from "../../generated/openapi";
import {DotLoading, Image, InfiniteScroll, List, Modal, Space, Tag, Toast} from "antd-mobile";
import TreeForm from "../../components/Map/TreeForm";
import {ctShortTreeOf, CtTreeShort} from "../../components/Map/Models/CtTreeShort";
import {useUser} from "../../app/hooks";

const MyTreesPage: React.FC = () => {
  const user = useUser()
  const [data, setData] = useState<CtTreeShort[]>([])

  const getTreeTag = (tree: CtTreeShort) => {
    const status = tree.status;
    let color
    if (status === TreeStatus.New) {
      color = "primary"
    } else if (status === TreeStatus.ToApprove) {
      color = "warning"
    } else if (status === TreeStatus.Approved) {
      color = "success"
    } else if (status === TreeStatus.Deleted) {
      color = "default"
    } else {
      color = "default"
    }
    return <Tag color={color} fill="outline">{status}</Tag>
  }

  const updateTableRecord = (record: CtTreeShort | CtTree) => {
    const index = data.findIndex((tree) => record.id === tree.id)
    if (index > -1) {
      const newData = [...data]
      const item = newData[index]
      newData.splice(index, 1, {
        ...item,
        ...record as CtTreeShort,
      })
      setData(newData)
    }
  }

  const [hasMore, setHasMore] = useState(true)

  const loadMore = async () => {
    let cursorPosition
    if (data.length > 0) {
      cursorPosition = data[data.length - 1].id
    }
    let append = await api.tree.getAllUserTrees({limit: 30, cursorPosition: cursorPosition})
        .then(request => request.map(tree => ctShortTreeOf(tree)))

    setData(val => [...val, ...append])
    setHasMore(append.length > 0)
  };

  return (
      <div>
        <List>
          {data.map((item) => (
              <List.Item
                  key={item.id}
                  prefix={item.fileUrls &&
                      <Image
                          width={44}
                          height={44}
                          style={{borderRadius: 4}}
                          fit='cover'
                          src={item.fileUrls[0]}
                      />}
                  description={`${item.latitude} ${item.longitude}`}
                  onClick={() =>
                      api.tree.getTreeById({id: item.id}).then(treeResponse => {
                        api.tree.getAllAttachedFiles({treeId: item.id}).then((filesResponse) => {
                          const tree = ctTreeOf(treeResponse, filesResponse)
                          const modal = Modal.show({
                            content: <TreeForm
                                initial={tree}
                                editable={isTreeEditable(item, user)}
                                onSave={(tree) => {
                                  updateTree(tree, tree.status, () => {
                                        updateTableRecord(tree)
                                        Toast.show({content: "Tree was saved!", position: "top"})
                                      }
                                  )
                                }}
                                onPublish={(tree) => {
                                  updateTree(tree, TreeStatus.ToApprove, () => {
                                    Toast.show({content: "Tree was published!", position: "top"})
                                    updateTableRecord(tree)
                                    modal.close()
                                  })
                                }}
                                onDelete={(tree) => deleteTree(tree, () => {
                                  tree.status = "DELETED"
                                  updateTableRecord(tree)
                                  modal.close()
                                })}
                                isDeletable={isTreeDeletable(item, user)}
                                onCancel={() => modal.close()}
                            />,
                            closeOnMaskClick: true
                          })
                        })
                      })
                  }
              >
                <Space justify="center">
                  <span style={{verticalAlign: "middle"}}>{item.id}</span>
                  {getTreeTag(item)}
                </Space>
              </List.Item>
          ))}
        </List>
        <InfiniteScroll aria-valuetext="" loadMore={loadMore} hasMore={hasMore}>
          <div>
            {hasMore && <>
              <span>Loading</span>
              <DotLoading/>
            </>}
          </div>
        </InfiniteScroll>
      </div>
  )
};

export default MyTreesPage;
