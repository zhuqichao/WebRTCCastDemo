import jquery from "../../static/js/jquery.min"

export default {
  postMethod: function (hostname, param, url) {
    url = hostname + '/' + url
    return jquery.ajax({
      url: url,
      type: 'POST',
      dataType: 'json',
      data: JSON.stringify(param),
      async: false,
      contentType: 'application/json;charset=UTF-8',
      success: function (data) {
      },
      error: function (e) {
      }
    }).responseJSON
  },
  getMethod: function (hostname, param, url) {
    url = hostname + '/' + url
    return jquery.ajax({
      url: url,
      type: 'GET',
      dataType: 'json',
      data: param,
      async: false,
      contentType: 'application/json;charset=UTF-8',
      success: function (data) {
      },
      error: function (e) {
      }
    }).responseJSON
  }
}
