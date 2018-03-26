using Microsoft.Azure.Mobile.Server;

namespace nnrCarMonitoringService.DataObjects
{
    public class TodoItem : EntityData
    {
        public string Text { get; set; }

        public bool Complete { get; set; }
        public string Url { get; set; }

    }
}